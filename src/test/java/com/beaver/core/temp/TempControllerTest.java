package com.beaver.core.temp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TempControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void tempEndpoint_ShouldReturnStaticMessage() throws Exception {
        mockMvc.perform(get("/temp"))
                .andExpect(status().isOk())
                .andExpect(content().string("This is a temporary endpoint for testing purposes."));
    }
}