package uk.co.playerdata.reactnativemcumanager.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.runtime.mcumgr.response.McuMgrResponse;

public class HashResponse extends McuMgrResponse {
    @JsonProperty("type")
    public String type;

    @JsonProperty("off")
    public int off;

    @JsonProperty("len")
    public int len;

    // Note that the output property can be an unsigned int or a byte string, depending on the type
    // of hash or checksum. For crc32 it is uint and for sha256 it is bstr.
    // For the Picoo use case the sha256 hash is used, therefor the type of the output property
    // is set to byte[]. It needs to be investigated how to dynamically specify this type.
    @JsonProperty("output")
    public byte[] output;
}
