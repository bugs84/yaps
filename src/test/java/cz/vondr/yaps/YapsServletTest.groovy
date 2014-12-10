package cz.vondr.yaps

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.mockito.Mockito.verify

@RunWith(MockitoJUnitRunner.class)
class YapsServletTest {

    @Mock
    HttpServletRequest request
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    HttpServletResponse response

    @Before
    void setup() {
    }

    @Test
    void 'groovy sample test'() {
        new YapsServlet().service(request, response);

        verify(response).status = 200
    }
}
