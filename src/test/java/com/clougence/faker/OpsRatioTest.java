package com.dodo.apate;

import java.io.IOException;

import org.junit.Test;

import com.dodo.apate.utils.RandomRatio;
import com.dodo.apate.utils.RatioUtils;

public class OpsRatioTest {

    @Test
    public void iteratorlocks() throws IOException {
        RandomRatio<OpsType> opsRatio = RatioUtils.passerByConfig("I#20,UPDATE# 12;d # 33");
        System.out.println();
    }
}
