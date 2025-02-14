/*
 * Adaptado para o protocolo iTR120
 */
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;

public class ITR120ProtocolDecoder extends BaseProtocolDecoder {

    private static final int MSG_LOGIN = 0x01;
    private static final int MSG_HEARTBEAT = 0x03;
    private static final int MSG_LOCATION = 0x12;
    private static final int MSG_COMMAND = 0x80;

    public ITR120ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.readUnsignedShort() != 0x2828) {
            return null;
        }

        int pid = buf.readUnsignedByte();
        int size = buf.readUnsignedShort();
        int sequence = buf.readUnsignedShort();

        Position position = new Position(getProtocolName());
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);

        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        switch (pid) {
            case MSG_LOGIN:
                return decodeLogin(channel, buf, deviceSession, sequence);
            case MSG_HEARTBEAT:
                return decodeHeartbeat(channel, buf, position, sequence);
            case MSG_LOCATION:
                return decodeLocation(channel, buf, position, sequence);
            case MSG_COMMAND:
                return decodeCommandResponse(channel, buf, position, sequence);
            default:
                sendAck(channel, pid, sequence);
                return null;
        }
    }

    private Object decodeLogin(Channel channel, ByteBuf buf, DeviceSession deviceSession, int sequence) {
        String imei = String.format("%015d", buf.readLong());
        buf.skipBytes(15); // Ignorar campos não essenciais
        sendAck(channel, MSG_LOGIN, sequence);
        return null;
    }

    private Object decodeHeartbeat(Channel channel, ByteBuf buf, Position position, int sequence) {
        position.set(Position.KEY_STATUS, buf.readUnsignedShort());
        sendAck(channel, MSG_HEARTBEAT, sequence);
        return position;
    }

    private Object decodeLocation(Channel channel, ByteBuf buf, Position position, int sequence) {
        position.setTime(new Date(buf.readUnsignedInt() * 1000));
        buf.readUnsignedByte(); // Mask
        position.setLatitude(buf.readInt() / 1800000.0);
        position.setLongitude(buf.readInt() / 1800000.0);
        sendAck(channel, MSG_LOCATION, sequence);
        return position;
    }

    private Object decodeCommandResponse(Channel channel, ByteBuf buf, Position position, int sequence) {
        sendAck(channel, MSG_COMMAND, sequence);
        return position;
    }

    private void sendAck(Channel channel, int pid, int sequence) {
        ByteBuf ack = Unpooled.buffer();
        ack.writeShort(0x2828);
        ack.writeByte(pid);
        ack.writeShort(0x0002);
        ack.writeShort(sequence);
        channel.writeAndFlush(new NetworkMessage(ack, channel.remoteAddress()));
    }
}
