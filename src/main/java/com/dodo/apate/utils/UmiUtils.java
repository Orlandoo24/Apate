/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dodo.apate.utils;

import java.sql.Connection;
import java.sql.SQLException;

import com.dodo.adapter.adbmysql.AdbMySqlSchemaPlugin;
import com.dodo.adapter.adbmysql.meta.AdbMySQLMetadataProvider;
import com.dodo.adapter.adbmysql.umi.AdbMySQLUmiService;
import com.dodo.adapter.clickhouse.ClickHouseSchemaPlugin;
import com.dodo.adapter.clickhouse.meta.ClickHouseMetadataProvider;
import com.dodo.adapter.clickhouse.umi.ClickHouseUmiService;
import com.dodo.adapter.db2.Db2SchemaPlugin;
import com.dodo.adapter.db2.meta.Db2MetadataProvider;
import com.dodo.adapter.db2.umi.Db2UmiService;
import com.dodo.adapter.dm.DmSchemaPlugin;
import com.dodo.adapter.dm.meta.DmMetadataProvider;
import com.dodo.adapter.dm.umi.DmUmiService;
import com.dodo.adapter.doris.DorisSchemaPlugin;
import com.dodo.adapter.doris.meta.DorisMetadataProvider;
import com.dodo.adapter.doris.umi.DorisUmiService;
import com.dodo.adapter.hana.HanaSchemaPlugin;
import com.dodo.adapter.hana.meta.HanaMetadataProvider;
import com.dodo.adapter.hana.umi.HanaUmiService;
import com.dodo.adapter.mysql.MySqlSchemaPlugin;
import com.dodo.adapter.mysql.meta.MySQLMetadataProvider;
import com.dodo.adapter.mysql.umi.MySQLUmiService;
import com.dodo.adapter.oceanbase.OceanBaseSchemaPlugin;
import com.dodo.adapter.oceanbase.meta.OceanBaseMetadataProvider;
import com.dodo.adapter.oceanbase.umi.OceanBaseUmiService;
import com.dodo.adapter.oracle.OracleSchemaPlugin;
import com.dodo.adapter.oracle.meta.OracleMetadataProvider;
import com.dodo.adapter.oracle.umi.OracleUmiService;
import com.dodo.adapter.polardbmy.PolarDBMySchemaPlugin;
import com.dodo.adapter.polardbmy.meta.PolarDBMyMetadataProvider;
import com.dodo.adapter.polardbmy.umi.PolarDBMyUmiService;
import com.dodo.adapter.polardbpg.PolarDBPgSchemaPlugin;
import com.dodo.adapter.polardbpg.meta.PolarDBPgMetadataProvider;
import com.dodo.adapter.polardbpg.umi.PolarDBPgUmiService;
import com.dodo.adapter.postgre.PostgreSchemaPlugin;
import com.dodo.adapter.postgre.meta.PostgresMetadataProviderExt;
import com.dodo.adapter.postgre.umi.PostgreUmiService;
import com.dodo.adapter.redshift.RedshiftSchemaPlugin;
import com.dodo.adapter.redshift.meta.RedshiftMetadataProvider;
import com.dodo.adapter.redshift.umi.RedshiftUmiService;
import com.dodo.adapter.sqlserver.SqlServerSchemaPlugin;
import com.dodo.adapter.sqlserver.meta.SqlServerMetadataProvider;
import com.dodo.adapter.sqlserver.umi.SqlServerUmiService;
import com.dodo.adapter.starrocks.StarRocksSchemaPlugin;
import com.dodo.adapter.starrocks.meta.StarRocksMetadataProvider;
import com.dodo.adapter.starrocks.umi.StarRocksUmiService;
import com.dodo.adapter.tidb.TiDBSchemaPlugin;
import com.dodo.adapter.tidb.meta.TiDBMetadataProvider;
import com.dodo.adapter.tidb.umi.TiDBUmiService;
import com.dodo.apate.apateConfig;
import com.dodo.schema.SchemaFramework;
import com.dodo.schema.umi.service.rdb.RdbUmiService;
import com.dodo.utils.function.ESupplier;

/**
 * 随机查询 SqlDialect 实现
 * @version : 2020-10-31
 * @author 
 */
public class UmiUtils {

    public static RdbUmiService newUmiService(ESupplier<Connection, SQLException> connection, apateConfig config) {
        if (config.getCustomFetchMeta() != null) {
            return config.getCustomFetchMeta();
        }

        switch (config.getDbType()) {
            case MySQL:
                SchemaFramework.install(new MySqlSchemaPlugin());
                return new MySQLUmiService(() -> new MySQLMetadataProvider(connection));
            case Oracle:
                SchemaFramework.install(new OracleSchemaPlugin());
                return new OracleUmiService(() -> new OracleMetadataProvider(connection));
            case PostgreSQL:
                SchemaFramework.install(new PostgreSchemaPlugin());
                return new PostgreUmiService(() -> new PostgresMetadataProviderExt(connection));
            case SqlServer:
                SchemaFramework.install(new SqlServerSchemaPlugin());
                return new SqlServerUmiService(() -> new SqlServerMetadataProvider(connection));
            case Dameng:
                SchemaFramework.install(new DmSchemaPlugin());
                return new DmUmiService(() -> new DmMetadataProvider(connection));
            case TiDB:
                SchemaFramework.install(new TiDBSchemaPlugin());
                return new TiDBUmiService(() -> new TiDBMetadataProvider(connection));
            case Db2:
                SchemaFramework.install(new Db2SchemaPlugin());
                return new Db2UmiService(() -> new Db2MetadataProvider(connection));
            case ClickHouse:
                SchemaFramework.install(new ClickHouseSchemaPlugin());
                return new ClickHouseUmiService(() -> new ClickHouseMetadataProvider(connection));
            case OceanBase:
                SchemaFramework.install(new OceanBaseSchemaPlugin());
                return new OceanBaseUmiService(() -> new OceanBaseMetadataProvider(connection));
            case AdbForMySQL:
                SchemaFramework.install(new AdbMySqlSchemaPlugin());
                return new AdbMySQLUmiService(() -> new AdbMySQLMetadataProvider(connection));
            case Doris:
                SchemaFramework.install(new DorisSchemaPlugin());
                return new DorisUmiService(() -> new DorisMetadataProvider(connection));
            case StarRocks:
                SchemaFramework.install(new StarRocksSchemaPlugin());
                return new StarRocksUmiService(() -> new StarRocksMetadataProvider(connection));
            case PolarDBMySQL:
                SchemaFramework.install(new PolarDBMySchemaPlugin());
                return new PolarDBMyUmiService(() -> new PolarDBMyMetadataProvider(connection));
            case PolarDBPostgre:
                SchemaFramework.install(new PolarDBPgSchemaPlugin());
                return new PolarDBPgUmiService(() -> new PolarDBPgMetadataProvider(connection));
            case Hana:
                SchemaFramework.install(new HanaSchemaPlugin());
                return new HanaUmiService(() -> new HanaMetadataProvider(connection.get()));
            case Redshift:
                SchemaFramework.install(new RedshiftSchemaPlugin());
                return new RedshiftUmiService(() -> new RedshiftMetadataProvider(connection.get()));
            default:
                throw new UnsupportedOperationException(config.getDbType() + " No UmiService was matched");
        }
    }

}
