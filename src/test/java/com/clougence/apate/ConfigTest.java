package com.dodo.apate;

import java.util.List;

import org.junit.Test;

import com.dodo.apate.generator.*;
import com.dodo.apate.realdb.DsUtils;

public class ConfigTest {

    @Test
    public void insertTest() throws Exception {
        apateFactory apateFactory = new apateFactory(DsUtils.dsMySql());
        apateRepository producer = new apateRepository(apateFactory);

        apateTable table = producer.addTable(null, null, "tb_user");
        table.setInsertPolitic(SqlPolitic.RandomCol);

        for (int j = 0; j < 10; j++) {
            List<BoundQuery> boundQueries = producer.generator(OpsType.Insert);

            for (BoundQuery query : boundQueries) {
                System.out.println(query.getSqlString());
            }
        }
    }

    @Test
    public void deleteTest() throws Exception {
        apateFactory apateFactory = new apateFactory(DsUtils.dsMySql());
        apateRepository producer = new apateRepository(apateFactory);

        apateTable table = producer.addTable(null, null, "tb_user");
        table.setWherePolitic(SqlPolitic.RandomCol);

        for (int j = 0; j < 10; j++) {
            List<BoundQuery> boundQueries = producer.generator(OpsType.Delete);

            for (BoundQuery query : boundQueries) {
                System.out.println(query.getSqlString());
            }
        }
    }

    @Test
    public void updateTest() throws Exception {
        apateFactory apateFactory = new apateFactory(DsUtils.dsMySql());
        apateRepository producer = new apateRepository(apateFactory);

        apateTable table = producer.addTable(null, null, "tb_user");
        table.setWherePolitic(SqlPolitic.RandomCol);
        table.setUpdateSetPolitic(SqlPolitic.RandomCol);

        for (int j = 0; j < 10; j++) {
            List<BoundQuery> boundQueries = producer.generator(OpsType.Delete);

            for (BoundQuery query : boundQueries) {
                System.out.println(query.getSqlString());
            }
        }
    }
}
