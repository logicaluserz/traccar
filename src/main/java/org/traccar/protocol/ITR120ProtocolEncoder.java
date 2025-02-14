/*
 * Adaptado para o protocolo iTR120
 */
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;

public class ITR120ProtocolEncoder extends BaseProtocolEncoder {

    public ITR120ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeCommandContent(long deviceId, String content, int sequence) {

        ByteBuf buf = Unpooled.buffer();

        // Cabeçalho do pacote (0x28 0x28)
        buf.writeShort(0x2828);

        // PID da instrução (0x80)
        buf.writeByte(0x80);

        // Tamanho: Sequence (2) + Type (1) + UID (4) + conteúdo
        int contentLength = content.getBytes(StandardCharsets.UTF_8).length;
        int size = 2 + 1 + 4 + contentLength;
        buf.writeShort(size);

        // Número de sequência (gerenciar conforme necessidade)
        buf.writeShort(sequence);

        // Tipo de instrução (0x01 = comando para dispositivo)
        buf.writeByte(0x01);

        // UID (exemplo fixo, idealmente único por comando)
        buf.writeInt(0x00000001);

        // Conteúdo do comando (ex: "RELAY,1#")
        buf.writeBytes(content.getBytes(StandardCharsets.UTF_8));

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        // Gerenciar sequência (exemplo fixo, substituir por lógica adequada)
        int sequence = 1;

        switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP:
                return encodeCommandContent(
                    command.getDeviceId(), "RELAY,1#", sequence); // Comando para desligar
            case Command.TYPE_ENGINE_RESUME:
                return encodeCommandContent(
                    command.getDeviceId(), "RELAY,0#", sequence); // Comando para ligar
            case Command.TYPE_CUSTOM:
                return encodeCommandContent(
                    command.getDeviceId(), command.getString(Command.KEY_DATA), sequence); // Comando personalizado
            default:
                return null; // Comando não suportado
        }
    }
}
