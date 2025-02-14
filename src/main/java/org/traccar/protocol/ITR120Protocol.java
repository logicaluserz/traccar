/*
 * Adaptado para o protocolo iTR120
 */
package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;
import org.traccar.model.Command;

import jakarta.inject.Inject;

public class ITR120Protocol extends BaseProtocol {

    @Inject
    public ITR120Protocol(Config config) {
        // Definir comandos suportados pelo protocolo iTR120
        setSupportedDataCommands(
                Command.TYPE_ENGINE_STOP,
                Command.TYPE_ENGINE_RESUME,
                Command.TYPE_CUSTOM);
        
        // Adicionar servidor para comunicação TCP
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                // Configurar os handlers do pipeline
                pipeline.addLast(new ITR120FrameDecoder()); // Decodificador de quadros
                pipeline.addLast(new ITR120ProtocolEncoder(ITR120Protocol.this)); // Encoder de comandos
                pipeline.addLast(new ITR120ProtocolDecoder(ITR120Protocol.this)); // Decodificador de pacotes
            }
        }); // <-- No trailing spaces here
    }
}
