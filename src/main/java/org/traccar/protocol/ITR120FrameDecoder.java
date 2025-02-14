/*
 * Adaptado para o protocolo iTR120
 */
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.traccar.BaseFrameDecoder;

public class ITR120FrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        // Verificar se há bytes suficientes para ler o cabeçalho básico (5 bytes)
        if (buf.readableBytes() < 5) {
            return null;
        }

        // Verificar marcador inicial 0x28 0x28
        if (buf.getUnsignedByte(buf.readerIndex()) != 0x28 ||
            buf.getUnsignedByte(buf.readerIndex() + 1) != 0x28) {
            return null;
        }

        // Ler o campo Size (bytes 3-4, big-endian)
        int size = buf.getUnsignedShort(buf.readerIndex() + 3);
        int totalLength = 5 + size; // Tamanho total do pacote

        // Verificar se o buffer contém todos os bytes necessários
        if (buf.readableBytes() >= totalLength) {
            return buf.readRetainedSlice(totalLength);
        }

        return null; // Aguardar mais dados
    }
}
