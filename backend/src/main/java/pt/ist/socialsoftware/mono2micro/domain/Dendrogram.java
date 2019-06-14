package pt.ist.socialsoftware.mono2micro.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.ist.socialsoftware.mono2micro.utils.Pair;

public class Dendrogram {
	private String name;
	private List<Graph> graphs = new ArrayList<>();
	private List<Controller> controllers = new ArrayList<>();
	private List<Entity> entities = new ArrayList<>();
	private String linkageType;
	private String accessMetricWeight;
	private String readWriteMetricWeight;
	private String sequenceMetricWeight;

	public Dendrogram() {
	}

	public Dendrogram(String name, String linkageType) {
		this.name = name;
		this.linkageType = linkageType;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLinkageType() {
		return this.linkageType;
	}

	public void setLinkageType(String linkageType) {
		this.linkageType = linkageType;
	}

	public String getAccessMetricWeight() {
		return this.accessMetricWeight;
	}

	public String getReadWriteMetricWeight() {
		return this.readWriteMetricWeight;
	}

	public String getSequenceMetricWeight() {
		return this.sequenceMetricWeight;
	}

	public void setClusteringMetricWeight(String accessMetricWeight, String readWriteMetricWeight, String sequenceMetricWeight) {
		this.accessMetricWeight = accessMetricWeight;
		this.readWriteMetricWeight = readWriteMetricWeight;
		this.sequenceMetricWeight = sequenceMetricWeight;
	}

	public List<Graph> getGraphs() {
		return this.graphs;
	}

	public boolean deleteGraph(String graphName) {
		for (int i = 0; i < this.graphs.size(); i++) {
			if (this.graphs.get(i).getName().equals(graphName)) {
				this.graphs.remove(i);
				return true;
			}
		}
		return false;
	}

	public List<String> getGraphsNames() {
		List<String> graphsNames = new ArrayList<>();
		for (Graph graph : this.graphs)
			graphsNames.add(graph.getName());
		return graphsNames;
	}

	public Graph getGraph(String graphName) {
		for (Graph graph : this.graphs) {
			if (graph.getName().equals(graphName))
				return graph;
		}
		return null;
	}

	public void addGraph(Graph graph) {
		this.graphs.add(graph);
		calculateMetrics(graph.getName());
	}

	public void mergeClusters(String graphName, String cluster1, String cluster2, String newName) {
		getGraph(graphName).mergeClusters(cluster1, cluster2, newName);
		calculateMetrics(graphName);
	}

	public boolean renameCluster(String graphName, String clusterName, String newName) {
		return getGraph(graphName).renameCluster(clusterName, newName);
	}

	public boolean renameGraph(String graphName, String newName) {
		if (getGraphsNames().contains(newName))
			return false;
		getGraph(graphName).setName(newName);
		return true;
	}

	public void splitCluster(String graphName, String clusterName, String newName, String[] entities) {
		getGraph(graphName).splitCluster(clusterName, newName, entities);
		calculateMetrics(graphName);
	}

	public void transferEntities(String graphName, String fromCluster, String toCluster, String[] entities) {
		getGraph(graphName).transferEntities(fromCluster, toCluster, entities);
		calculateMetrics(graphName);
	}

	public List<Controller> getControllers() {
		return this.controllers;
	}

	public void addController(Controller controller) {
		this.controllers.add(controller);
	}

	public Controller getController(String controllerName) {
		for (Controller controller : this.controllers) {
			if (controller.getName().equals(controllerName))
				return controller;
		}
		return null;
	}

	public boolean containsController(String controllerName) {
		return getController(controllerName) != null;
	}

	public List<Entity> getEntities() {
		return this.entities;
	}

	public void addEntity(Entity entity) {
		this.entities.add(entity);
	}

	public Entity getEntity(String entityName) {
		for (Entity entity : this.entities) {
			if (entity.getName().equals(entityName))
				return entity;
		}
		return null;
	}

	public boolean containsEntity(String entityName) {
		return getEntity(entityName) != null;
	}

	public Map<String,List<Controller>> getClusterControllers(String graphName) {
		Map<String,List<Controller>> clusterControllers = new HashMap<>();

		Graph graph = getGraph(graphName);
		for (Cluster cluster : graph.getClusters()) {
			List<Controller> touchedControllers = new ArrayList<>();
			for (Controller controller : this.controllers) {
				for (String controllerEntity : controller.getEntities().keySet()) {
					if (cluster.containsEntity(controllerEntity)) {
						touchedControllers.add(controller);
						break;
					}
				}
			}
			clusterControllers.put(cluster.getName(), touchedControllers);
		}
		return clusterControllers;
	}

	public Map<String,List<Cluster>> getControllerClusters(String graphName) {
		Map<String,List<Cluster>> controllerClusters = new HashMap<>();

		Graph graph = getGraph(graphName);
		for (Controller controller : this.controllers) {
			List<Cluster> touchedClusters = new ArrayList<>();
			for (Cluster cluster : graph.getClusters()) {
				for (String clusterEntity : cluster.getEntities()) {
					if (controller.containsEntity(clusterEntity)) {
						touchedClusters.add(cluster);
						break;
					}
				}
			}
			controllerClusters.put(controller.getName(), touchedClusters);
		}
		return controllerClusters;
	}

	public Map<String,List<Pair<Cluster,String>>> getControllerClustersSeq(String graphName) {
		Map<String,List<Pair<Cluster,String>>> controllerClustersSeq = new HashMap<>();

		Graph graph = getGraph(graphName);
		for (Controller controller : this.controllers) {
			List<Pair<Cluster,String>> touchedClusters = new ArrayList<>();
			Pair<Cluster,String> lastCluster = null;
			for (Pair<String,String> entityPair : controller.getEntitiesSeq()) {
				String entityName = entityPair.getFirst();
				String mode = entityPair.getSecond();
				Cluster clusterAccessed = graph.getClusterWithEntity(entityName);
				if (lastCluster == null){
					lastCluster = new Pair<Cluster,String>(clusterAccessed, mode);
					touchedClusters.add(new Pair<Cluster,String>(clusterAccessed, mode));
				} else if (lastCluster.getFirst().getName().equals(clusterAccessed.getName())) {
					if (!lastCluster.getSecond().contains(mode)) {
						lastCluster.setSecond("RW");
						touchedClusters.get(touchedClusters.size() - 1).setSecond("RW");
					}
				} else {
					lastCluster = new Pair<Cluster,String>(clusterAccessed, mode);
					touchedClusters.add(new Pair<Cluster,String>(clusterAccessed, mode));
				}
			}
			controllerClustersSeq.put(controller.getName(), touchedClusters);
		}
		return controllerClustersSeq;
	}

	public void calculateMetrics(String graphName) {
		getGraph(graphName).calculateMetrics(getClusterControllers(graphName), getControllerClustersSeq(graphName));
	}
}
