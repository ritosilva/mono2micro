package collectors;

import com.google.gson.*;
import org.apache.commons.io.FileUtils;
import spoon.DecompiledResource;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import util.Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;

public abstract class SpoonCollector {
    private int controllerCount;
    private String projectName;

    private JsonObject callSequence;
    private JsonArray entitiesSequence;
    private boolean foundAccess;

    private HashMap<String, Node> entitiesSequenceHashMap;
    protected String currentParentNodeId;

    // all project classes
    ArrayList<String> allEntities;

    // in FenixFramework: allDomainEntities = all classes that extend from .*_Base
    // in JPA: allDomainEntities = all @Entities @Embeddable @MappedSuperClass
    ArrayList<String> allDomainEntities;

    HashSet<CtClass> controllers;
    Map<SourcePosition, List<CtAbstractInvocation>> methodCallees;
    Map<String, List<CtType>> interfaces;

    Factory factory;
    Launcher launcher;

    SpoonCollector(int launcherChoice, String repoName, String projectPath) throws IOException {
        File decompiledDir = new File(Constants.DECOMPILED_SOURCES_PATH);
        if (decompiledDir.exists()) {
            FileUtils.deleteDirectory(decompiledDir);
        }

        callSequence = new JsonObject();
        controllers = new HashSet<>();
        allEntities = new ArrayList<>();
        allDomainEntities = new ArrayList<>();
        methodCallees = new HashMap<>();
        interfaces = new HashMap<>();
        foundAccess = false;

        this.projectName = repoName;

        switch (launcherChoice) {
            case Constants.LAUNCHER:
                launcher = new Launcher();
                launcher.addInputResource(projectPath);
                break;

            case Constants.MAVEN_LAUNCHER:
                launcher = new MavenLauncher(projectPath, MavenLauncher.SOURCE_TYPE.APP_SOURCE);
                break;

            case Constants.JAR_LAUNCHER:
                launcher = new Launcher();
                File projectDir = new File(projectPath);
                String[] extensions = new String[]{"jar"};
                Collection<File> packageFiles = FileUtils.listFiles(
                        projectDir,
                        extensions,
                        true
                );

                for (File packageFile : packageFiles) {
                    String packagePath = packageFile.getPath();
                    String decompiledPackageSourcesDir = Constants.DECOMPILED_SOURCES_PATH
                            + packagePath.substring(packagePath.lastIndexOf("/")+1)
                            + File.separator;
                    new File(decompiledPackageSourcesDir).mkdirs();

                    new DecompiledResource(
                            packagePath,
                            null,
                            null,
                            decompiledPackageSourcesDir
                    );
                    launcher.addInputResource(decompiledPackageSourcesDir);
                }
                break;
            default:
                System.exit(1);
                break;
        }

        launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
        launcher.getEnvironment().setCommentEnabled(false);
    }

    public void run() throws IOException {
        factory = launcher.getFactory();

        System.out.println("Processing Project: " + projectName);
        long startTime = System.currentTimeMillis();
        collectControllersAndEntities();

        controllerCount = 0;
        for (CtClass controller : controllers) {
            controllerCount++;
            processController(controller);
        }


        long elapsedTimeMillis = System.currentTimeMillis() - startTime;
        float elapsedTimeSec = elapsedTimeMillis/1000F;
        System.out.println("Complete. Elapsed time: " + elapsedTimeSec + " seconds");

        String fileName = projectName + ".json";
        storeJsonFile(Constants.COLLECTION_SAVE_PATH, fileName, callSequence);

        File file = new File(Constants.DECOMPILED_SOURCES_PATH);
        if (file.exists()) {
            FileUtils.deleteDirectory(file);
        }
    }

    private void processController(CtClass controller) {
        for (Object cM : controller.getMethods()) {
            CtMethod controllerMethod = (CtMethod) cM;

            //Check if the controller method return type == ActionForward
            if (controller.getSuperclass() != null && controller.getSuperclass().getSimpleName().contains("DispatchAction")) {
                if (!controllerMethod.getType().toString().contains("ActionForward"))
                    continue;
            } else {
                List<CtAnnotation<? extends Annotation>> annotations = controllerMethod.getAnnotations();
                if (!existsAnnotation(annotations, "RequestMapping") &&
                    !existsAnnotation(annotations, "GetMapping") &&
                    !existsAnnotation(annotations, "PostMapping") &&
                    !existsAnnotation(annotations, "PatchMapping") &&
                    !existsAnnotation(annotations, "PutMapping") &&
                    !existsAnnotation(annotations, "DeleteMapping")
                ) {
                    continue;
                }
            }

            String controllerFullName = controller.getSimpleName() + "." + controllerMethod.getSimpleName();
            System.out.println("Processing Controller: " + controllerFullName + "   " + controllerCount + "/" + controllers.size());

            /* TO REMOVE */
            if (!controllerFullName.contains("ToRemoveController"))
                return;

            entitiesSequence = new JsonArray();
            entitiesSequenceHashMap = new HashMap<>();
            Stack<SourcePosition> methodStack = new Stack<>();
            Stack<String> nextNodeIdStack = new Stack<>();
            foundAccess = false;

            // create graph node begin
            Node beginNode = createGraphNode(controllerMethod.toString());
            currentParentNodeId = beginNode.getId();
            methodCallDFS(controllerMethod, null, methodStack, nextNodeIdStack);

            if (foundAccess) {
                traverseGraph(controllerMethod.toString(), new ArrayList<>());
                callSequence.add(controller.getSimpleName() + "." + controllerMethod.getSimpleName(), entitiesSequence);
            }
        }
    }

    private void traverseGraph(String nodeId, List<JsonArray> localAccesses) {
        Node source = getGraphNode(nodeId);
        localAccesses.add(source.getEntitiesSequence());

        if (source.getEdges().size() == 0) {
            localAccesses.forEach(ja -> System.out.print(ja.toString()));
            System.out.println("");
        }
        else {
            for (Node node : source.getEdges())
                traverseGraph(node.getId(), localAccesses);
        }
    }

    abstract void methodCallDFS(CtExecutable callerMethod, CtAbstractInvocation prevCalleeLocation, Stack<SourcePosition> methodStack, Stack<String> nextNodeIdStack);

    abstract void collectControllersAndEntities();

    boolean existsAnnotation(List<CtAnnotation<? extends Annotation>> annotations, String annotationName) {
        for (CtAnnotation<? extends Annotation> a : annotations) {
            if ( a.getAnnotationType().getSimpleName().equals(annotationName) )
                return true;
        }
        return false;
    }

    private void storeJsonFile(String filepath, String fileName, JsonObject callSequence) {
        try {
            File filePath = new File(filepath);
            if (!filePath.exists()) {
                filePath.mkdirs();
            }

            FileWriter file = new FileWriter(new File(filePath, fileName));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            file.write(gson.toJson(callSequence));
            file.close();

            System.out.println("File '" + fileName + "' created at: " + filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Node createGraphNode(String id) {
        Node newNode = new Node(id);
        entitiesSequenceHashMap.put(id, newNode);
        return newNode;
    }

    Node getGraphNode(String id) {
        return entitiesSequenceHashMap.get(id);
    }

    void removeGraphNode(String id) {
        entitiesSequenceHashMap.remove(id);
    }

    void linkNodes(String originNodeId, String destNodeId) {
        entitiesSequenceHashMap.get(originNodeId).addEdge(entitiesSequenceHashMap.get(destNodeId));
    }

    void unlinkLast(String id) {
        entitiesSequenceHashMap.get(id).removeLastEdge();
    }

    void addEntitiesSequenceAccess(String simpleName, String mode) {
        JsonArray entityAccess = new JsonArray();
        entityAccess.add(simpleName);
        entityAccess.add(mode);
        entitiesSequence.add(entityAccess);
        entitiesSequenceHashMap.get(currentParentNodeId).addEntityAccess(entityAccess);
        foundAccess = true;
    }
}