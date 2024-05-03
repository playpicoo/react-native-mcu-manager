package uk.co.playerdata.reactnativemcumanager.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.runtime.mcumgr.response.McuMgrResponse;

public class StatusResponse extends McuMgrResponse {
    @JsonProperty("len")
    public int len;
}
