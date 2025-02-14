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

        if (buf.readableBytes() < 5) {
            return null;
        }

        if (buf.getUnsignedByte(buf.readerIndex()) != 0x28
                || buf.getUnsignedByte(buf.readerIndex() + 1) != 0x28) {
            return null;
        }

        int size = buf.getUnsignedShort(buf.readerIndex() + 3);
        int totalLength = 5 + size;

        if (buf.readableBytes() >= totalLength) {
            return buf.readRetainedSlice(totalLength);
        }

        return null;
    }
}
