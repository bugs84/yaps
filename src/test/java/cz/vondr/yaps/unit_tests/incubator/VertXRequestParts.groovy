package cz.vondr.yaps.unit_tests.incubator

import com.jcabi.http.request.JdkRequest
import cz.vondr.yaps.unit_tests.tool.JettyProxy
import cz.vondr.yaps.unit_tests.tool.VertXTarget
import org.junit.Test

class VertXRequestParts implements VertXTarget, JettyProxy {

    @Test
    void 'path is rewritten just writing test'() {
        targetHandler = { req ->
            println "absoluteURI".padLeft(14) + " - '${req.absoluteURI()}'"
            println "path".padLeft(14) + " - '${req.path()}'"
            println "uri".padLeft(14) + " - '${req.uri()}'"
            println "method".padLeft(14) + " - '${req.method()}'"
            println "query".padLeft(14) + " - '${req.query()}'"
            println "version".padLeft(14) + " - '${req.version()}'"
            println "remoteAddress".padLeft(14) + " - '${req.remoteAddress()}'"
            println "localAddress".padLeft(14) + " - '${req.localAddress()}'"
            println "params".padLeft(14) + " - '${req.params().toList()}'"
            println "headers".padLeft(14) + " - '${req.headers().toList()}'"

            req.response().end()
        }

        new JdkRequest("${proxyUrl}/context/path.txt?param1&foo=bar#fragment").fetch()

    }
}