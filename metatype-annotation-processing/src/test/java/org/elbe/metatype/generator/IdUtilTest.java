/**
 *
 */
package org.elbe.metatype.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author lbenno
 *
 */
class IdUtilTest {

    @Test
    void toId() {
        assertEquals("simpleName", IdUtil.toId("simpleName"));
    }

    @Test
    void toId_WithUnder() {
        assertEquals("ethz.id.knowhow.config.solr", IdUtil.toId("ethz_id_knowhow_config_solr"));
        assertEquals("double_under", IdUtil.toId("double__under"));
        assertEquals("double_under.path", IdUtil.toId("double__under_path"));
        assertEquals("double_under.path_part", IdUtil.toId("double__under_path__part"));
    }

    @Test
    void toId_WithDollar() throws Exception {
        assertEquals("namewithdollar", IdUtil.toId("name$with$dollar"));
        assertEquals("name$with$dollar", IdUtil.toId("name$$with$$dollar"));
        assertEquals("name-with-dollar", IdUtil.toId("name$_$with$_$dollar"));
        assertEquals("manyparts-with$dollar", IdUtil.toId("many$parts$_$with$$dollar"));
    }

    @Test
    void createId() throws Exception {
        assertEquals("configuration", IdUtil.createId("Configuration"));
        assertEquals("my.configuration", IdUtil.createId("MyConfiguration"));

        assertEquals("configuration", IdUtil.createId("CONfiguration"));
        assertEquals("my.vpn.configuration", IdUtil.createId("MyVPnConfiguration"));
    }

    @Test
    void toFieldName() throws Exception {
        assertEquals("ethzIdKnowhowConfigSolr", IdUtil.toFieldName("ethz_id_knowhow_config_solr"));
    }

}
