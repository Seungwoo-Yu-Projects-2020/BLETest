package com.swy2k.bletest.recyclerviews.BLEList;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RecyclerViewItem {
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    public static final int STATUS_CONNECTIBLE = 0;
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    public static final int STATUS_CONNECTING = 1;
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    public static final int STATUS_CONNECTED = 2;
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    public static final int STATUS_ERROR = 3;

    private String name;
    private String macAddress;
    private int status;
}
