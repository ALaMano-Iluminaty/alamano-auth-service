package com.alamano.auth.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void sinCabeceraGeneraUnUuid() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        String id = response.getHeader(CorrelationIdFilter.HEADER);
        assertNotNull(id);
        assertEquals(36, id.length());
    }

    @Test
    void unaCabeceraEnBlancoTambienGeneraUno() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(36, response.getHeader(CorrelationIdFilter.HEADER).length());
    }

    @Test
    void laCabeceraEntranteSePropagaYQuedaEnElMdcDuranteLaPeticion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "corr-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> enMdc = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> enMdc.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertEquals("corr-abc", response.getHeader(CorrelationIdFilter.HEADER));
        assertEquals("corr-abc", enMdc.get());
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY), "al terminar la peticion el MDC queda limpio");
    }
}
