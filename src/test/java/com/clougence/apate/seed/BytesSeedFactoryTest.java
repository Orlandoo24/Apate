package com.dodo.apate.seed;

import java.io.Serializable;
import java.util.function.Supplier;

import org.junit.Test;

import com.dodo.apate.seed.bytes.BytesSeedConfig;
import com.dodo.apate.seed.bytes.BytesSeedFactory;
import com.dodo.utils.HexadecimalUtils;

public class BytesSeedFactoryTest {

    @Test
    public void buildBytes_1() {
        BytesSeedFactory factory = new BytesSeedFactory();
        BytesSeedConfig genConfig = new BytesSeedConfig();
        genConfig.setMinLength(2);
        genConfig.setMaxLength(64);
        genConfig.setAllowNullable(false);

        Supplier<Serializable> bytesSupplier = factory.createSeed(genConfig);

        for (int i = 0; i < 10; i++) {
            System.out.println(HexadecimalUtils.bytes2hex((byte[]) bytesSupplier.get()));
        }
    }

    @Test
    public void buildBytes_2() {
        BytesSeedFactory factory = new BytesSeedFactory();
        BytesSeedConfig genConfig = new BytesSeedConfig();
        genConfig.setMinLength(2);
        genConfig.setMaxLength(64);
        genConfig.setAllowNullable(true);
        genConfig.setNullableRatio(10.0f);

        Supplier<Serializable> bytesSupplier = factory.createSeed(genConfig);

        for (int i = 0; i < 10; i++) {
            byte[] bytes = (byte[]) bytesSupplier.get();
            if (bytes == null) {
                System.out.println("@@NULL@@");
            } else {
                System.out.println(HexadecimalUtils.bytes2hex(bytes));
            }
        }
    }

}
