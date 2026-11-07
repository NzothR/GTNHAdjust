package com.gtnhadjust.machine.collector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CollectorMarkerPolicyTest {

    @Test
    public void recognizesBothTierAndIndividualDimensionMarkers() {
        assertTrue(CollectorMarkerPolicy.isMarker("gtnhadjust", "tier_dimension"));
        assertTrue(CollectorMarkerPolicy.isMarker("gtneioreplugin", "blockDimensionDisplay_Mo"));
        assertFalse(CollectorMarkerPolicy.isMarker("gtnhadjust", "tier_dimension_other"));
        assertFalse(CollectorMarkerPolicy.isMarker("other", "blockDimensionDisplay_Mo"));
    }
}
