/**
 * 
 */
package io.pkts.packet.sip.address.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.address.SipURI;

import org.junit.Before;
import org.junit.Test;


/**
 * @author jonas@jonasborjesson.com
 */
public class SipUriImplTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * Comparing two SIP URI's aren't super trivial. Make sure that we get it right!
     * 
     * @throws Exception
     */
    @Test
    public void testSipURIEquals() throws Exception {
        assertSipUriEquality("sip:alice@example.com", null, false);

        // rule no 1, sip and sips are never ever equal
        assertSipUriEquality("sip:alice@example.com", "sips:alice@example.com", false);

        // are equal
        assertSipUriEquality("sip:alice@example.com", "sip:alice@example.com", true);
        assertSipUriEquality("sip:Alice@example.com", "sip:Alice@example.com", true);
        assertSipUriEquality("sip:AliCe@example.com", "sip:AliCe@example.com", true);

        // are not equal because userinfo part is case sensitive
        assertSipUriEquality("sip:Alice@example.com", "sip:alice@example.com", false);
        assertSipUriEquality("sip:Alice@example.com", "sip:AlicE@example.com", false);

        assertSipUriEquality("sip:example.com", "sip:AlicE@example.com", false);
        assertSipUriEquality("sip:bob@example.com", "sip:example.com", false);
        assertSipUriEquality("sip:example.com", "sip:example.com", true);

        // host portion is case in-sensitive though
        assertSipUriEquality("sip:alice@example.com", "sip:alice@examPLE.com", true);
        assertSipUriEquality("sip:alice@Example.com", "sip:alice@examPLE.com", true);
        assertSipUriEquality("sip:alice@Example.coM", "sip:alice@examPLE.com", true);

        assertSipUriEquality("sip:alice@example.coM", "sip:alice@hello.com", false);

        // If specified the port has to match. If one URI specifies the default port
        // it still doesn't match a uri that does not specify the port.
        assertSipUriEquality("sip:alice@example.com:5060", "sip:alice@example.com", false);
        assertSipUriEquality("sip:alice@example.com", "sip:alice@example.com:5060", false);
        assertSipUriEquality("sip:alice@example.com:5060", "sip:alice@example.com:5060", true);
        assertSipUriEquality("sip:alice@example.com:1234", "sip:alice@example.com:1234", true);
        assertSipUriEquality("sip:alice@example.com:1234", "sip:alice@example.com:7893", false);

        // not equal because if method is specified they have to be specified in both
        assertSipUriEquality("sip:alice@example.com;method=REGISTER", "sip:alice@example.com", false);
        assertSipUriEquality("sip:alice@example.com;method=REGISTER", "sip:alice@example.com;method=INVITE", false);

        // verify ttl
        assertSipUriEquality("sip:alice@example.com;ttl=123", "sip:alice@example.com", false);
        assertSipUriEquality("sip:alice@example.com;ttl=123", "sip:alice@example.com;ttl=234", false);
        assertSipUriEquality("sip:alice@example.com;ttl=123", "sip:alice@example.com;ttl=123", true);

        // verify maddr
        assertSipUriEquality("sip:alice@example.com;ttl=123;maddr=239.255.255.1", "sip:alice@example.com;ttl=123",
                false);
        assertSipUriEquality("sip:alice@example.com;ttl=123;maddr=239.255.255.1",
                "sip:alice@example.com;maddr=239.255.255.1;ttl=123", true);
        assertSipUriEquality("sip:alice@example.com;maddr=239.255.255.1", "sip:alice@example.com;maddr=239.255.255.1",
                true);
        assertSipUriEquality("sip:alice@example.com;maddr=239.255.255.2", "sip:alice@example.com;maddr=239.255.255.1",
                false);

        // -----------------------------------------------------------------------
        // Tests taken from RFC3261 section 19.1.4
        //
        String a = "sip:carol@chicago.com";
        String b = "sip:carol@chicago.com;newparam=5";
        final String c = "sip:carol@chicago.com;security=on";
        assertSipUriEquality(a, b, true);
        assertSipUriEquality(b, a, true);
        assertSipUriEquality(a, c, true);
        assertSipUriEquality(b, c, true);
        assertSipUriEquality(c, b, true);


        // don't handle header parameters right now...
        // a = "sip:biloxi.com;transport=tcp;method=REGISTER?to=sip:bob%40biloxi.com";
        // b = "sip:biloxi.com;method=REGISTER;transport=tcp?to=sip:bob%40biloxi.com";

        a = "sip:biloxi.com;transport=tcp;method=REGISTER";
        b = "sip:biloxi.com;method=REGISTER;transport=tcp";
        assertSipUriEquality(a, b, true);
        assertSipUriEquality(b, a, true);

        // a = "sip:alice@atlanta.com?subject=project%20x&priority=urgent";
        // b = "sip:alice@atlanta.com?priority=urgent&subject=project%20x";
        a = "sip:alice@atlanta.com";
        b = "sip:alice@atlanta.com";
        assertSipUriEquality(a, b, true);
        assertSipUriEquality(b, a, true);

        // (different usernames)
        a = "sip:ALICE@AtLanTa.CoM;Transport=udp";
        b = "sip:alice@AtLanTa.CoM;Transport=UDP";
        assertSipUriEquality(a, b, false);
        assertSipUriEquality(b, a, false);

        // (can resolve to different ports)
        a = "sip:bob@biloxi.com";
        b = "sip:bob@biloxi.com:5060";
        assertSipUriEquality(a, b, false);
        assertSipUriEquality(b, a, false);

        // (can resolve to different transports)
        a = "sip:bob@biloxi.com";
        b = "sip:bob@biloxi.com;transport=udp";
        assertSipUriEquality(a, b, false);
        assertSipUriEquality(b, a, false);

        // (can resolve to different port and transports)
        a = "sip:bob@biloxi.com";
        b = "sip:bob@biloxi.com:6000;transport=tcp";
        assertSipUriEquality(a, b, false);
        assertSipUriEquality(b, a, false);

        // (different header component)
        a = "sip:carol@chicago.com";
        b = "sip:carol@chicago.com?Subject=next%20meeting";
        // assertSipUriEquality(a, b, false);
        // assertSipUriEquality(b, a, false);

        // (even though that's what phone21.boxesbybob.com resolves to)
        a = "sip:bob@phone21.boxesbybob.com";
        b = "sip:bob@192.0.2.4";
        assertSipUriEquality(a, b, false);
        assertSipUriEquality(b, a, false);

        // Note that equality is not transitive:

        // are equivalent
        a = "sip:carol@chicago.com";
        b = "sip:carol@chicago.com;security=on";
        assertSipUriEquality(a, b, true);
        assertSipUriEquality(b, a, true);

        // are equivalent
        a = "sip:carol@chicago.com";
        b = "sip:carol@chicago.com;security=off";
        assertSipUriEquality(a, b, true);
        assertSipUriEquality(b, a, true);

        // are not equivalent
        a = "sip:carol@chicago.com;security=on";
        b = "sip:carol@chicago.com;security=off";
        assertSipUriEquality(a, b, false);
        assertSipUriEquality(b, a, false);
    }

    @Test
    public void testAddParameters() throws Exception {
        final SipURI uri = SipURI.frame(Buffers.wrap("sip:hello@10.0.1.5:51945;ob"));
        assertThat(uri.toString(), is("sip:hello@10.0.1.5:51945;ob"));
        uri.setParameter("expires", 500);
        assertThat(uri.toString(), is("sip:hello@10.0.1.5:51945;ob;expires=500"));

        final SipURI clone = uri.clone();
        assertThat(clone.getPort(), is(51945));
        assertThat(clone.getParameter("ob"), is(Buffers.EMPTY_BUFFER));
        assertThat(clone.getParameter("expires").toString(), is("500"));

    }

    /**
     * We can create a new builder off of another {@link SipURI} and by default
     * the builder will only copy user, host, port and transport along with if it is
     * a sip or sips. Any other parameters are not copied.
     * 
     * @throws Exception
     */
    @Test
    public void testBuilderBasedOffOfSipURI() throws Exception {
        assertBuildClone("sip:alice@aboutsip.com");
        assertBuildClone("sips:alice@aboutsip.com");
        assertBuildClone("sip:alice@aboutsip.com:5098");
        assertBuildClone("sip:alice@aboutsip.com:5098;transport=tcp");
        assertBuildClone("sip:alice@aboutsip.com;transport=udp");
        assertBuildClone("sip:aboutsip.com:5098;transport=tcp");
        assertBuildClone("sips:aboutsip.com:5098;transport=tcp");

        assertBuildClone("sips:aboutsip.com:5098;transport=tcp;apa=fup");
        assertBuildClone("sips:nisse@aboutsip.com;apa=fup");
        assertBuildClone("sips:nisse@aboutsip.com;apa=fup;transport=udp;kalles=kaviar");
    }

    /**
     * Comparing that the uri 'toParse' is equal to the result after we have built a new
     * one using {@link SipURI#with(SipURI)}.
     * 
     * @param toParse
     * @throws Exception
     */
    private void assertBuildClone(final String toParse) throws Exception {
        final Buffer buffer = Buffers.wrap(toParse);
        final SipURI uri = SipURI.frame(buffer);
        final SipURI clone = SipURI.with(uri).build();
        assertThat(uri, is(clone));
    }

    @Test
    public void testFramingSipURI() throws Exception {
        assertSipUri("sip:alice@example.com:5090", "alice", "example.com", 5090);
        assertSipUri("sip:alice@example.com", "alice", "example.com", -1);
        assertSipUri("sip:example.com", "", "example.com", -1);
        assertSipUri("sip:example.com:4;transport=udp", "", "example.com", 4);
        assertSipUri("sip:alice@example.com;transport=udp", "alice", "example.com", -1);
        assertSipUri("sip:a@example.com;transport=udp", "a", "example.com", -1);
        assertSipUri("sip:alice@example.com:5555?hello=world", "alice", "example.com", 5555);
    }

    /**
     * Make sure that we can set the port as expected
     * 
     * @throws Exception
     */
    @Test
    public void testSetPort() throws Exception {
        assertSetPort("sip:alice@example.com", 9999, "sip:alice@example.com:9999");
        assertSetPort("sip:alice@example.com:8888", 7777, "sip:alice@example.com:7777");
        assertSetPort("sip:alice@example.com:7", 8, "sip:alice@example.com:8");
        assertSetPort("sip:alice@example.com:7;transport=udp", 8, "sip:alice@example.com:8;transport=udp");
        assertSetPort("sip:alice@example.com;transport=tcp&hello=world", 9999,
                "sip:alice@example.com:9999;transport=tcp&hello=world");
    }

    private void assertSetPort(final String toParse, final int port, final String expected) throws Exception {
        final SipURI uri = SipURI.frame(Buffers.wrap(toParse));
        uri.setPort(port);
        assertThat(uri.getPort(), is(port));
        assertThat(uri.toString(), is(expected));

    }

    /**
     * Helper method for comparing two SipURIs and two URI's that are equal must also have its
     * hashCode equal. Note though, if two URI's are NOT equal, their hash code may still be the
     * same so in those cases we don't check since if a hash collision would be resolved using
     * equals-method.
     * 
     * @param uriOne
     * @param uriTwo
     * @param equals
     * @throws Exception
     */
    private void assertSipUriEquality(final String uriOne, final String uriTwo, final boolean equals) throws Exception {
        final SipURI one = SipURI.frame(Buffers.wrap(uriOne));
        final SipURI two = uriTwo != null ? SipURI.frame(Buffers.wrap(uriTwo)) : null;
        assertThat(one.equals(two), is(equals));
        if (equals && uriOne != null && uriTwo != null) {
            assertThat(one.hashCode() == two.hashCode(), is(equals));
        }
    }

    /**
     * Helper method for ensuring that we parse SIP Uri's correctly
     * 
     * @param toParse
     * @param expectedUser
     * @param expectedHost
     * @param expectedPort
     * @throws Exception
     */
    private void assertSipUri(final String toParse, final String expectedUser, final String expectedHost,
            final int expectedPort) throws Exception {
        final Buffer buffer = Buffers.wrap(toParse);
        final SipURI uri = SipURI.frame(buffer);
        assertThat(uri.isSipURI(), is(true));
        assertThat(uri.getUser().toString(), is(expectedUser));
        assertThat(uri.getHost().toString(), is(expectedHost));
        assertThat(uri.getPort(), is(expectedPort));
    }

}
