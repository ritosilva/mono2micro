package pt.ist.socialsoftware.mono2micro.dto;

import pt.ist.socialsoftware.mono2micro.domain.Graph;
import pt.ist.socialsoftware.mono2micro.utils.Constants;

public class AnalyserDto {
    private Graph expert;
    private String profile;
    private int requestLimit;
    // only used when the codebase "is dynamic" aka !isStatic()
    private int tracesMaxLimit; // default is 0 which means, no limit
    private Constants.TraceType traceType = Constants.TraceType.ALL;

    public Graph getExpert() { return expert; }

    public void setExpert(Graph expert) {
        this.expert = expert;
    }

    public String getProfile() { return profile; }

    public void setProfile(String profile) { this.profile = profile; }

    public int getRequestLimit() { return requestLimit; }

    public void setRequestLimit(int requestLimit) { this.requestLimit = requestLimit; }

    public int getTracesMaxLimit() { return tracesMaxLimit; }

    public void setTracesMaxLimit(int tracesMaxLimit) { this.tracesMaxLimit = tracesMaxLimit; }

    public Constants.TraceType getTypeOfTraces() { return traceType; }

    public void setTypeOfTraces(Constants.TraceType traceType) { this.traceType = traceType; }
}
