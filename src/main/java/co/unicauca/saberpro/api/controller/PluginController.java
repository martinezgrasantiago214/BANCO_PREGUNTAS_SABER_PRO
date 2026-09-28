package co.unicauca.saberpro.api.controller;

import co.unicauca.saberpro.api.dto.PluginResponseDTO;
import co.unicauca.saberpro.microkernel.core.QuestionMicrokernel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone los plugins que el microkernel cargo por Reflexion desde
 * plugins.properties: los valores validos del campo "type" al crear preguntas.
 */
@RestController
@RequestMapping("/api/plugins")
public class PluginController {

    private final QuestionMicrokernel microkernel;

    public PluginController(QuestionMicrokernel microkernel) {
        this.microkernel = microkernel;
    }

    @GetMapping
    public List<PluginResponseDTO> listPlugins() {
        return microkernel.getPlugins().stream()
                .map(p -> new PluginResponseDTO(p.getName(), p.getClass().getName()))
                .toList();
    }
}
