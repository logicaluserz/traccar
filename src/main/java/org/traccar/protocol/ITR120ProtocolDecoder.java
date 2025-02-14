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
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class ITR120ProtocolDecoder extends BaseProtocolDecoder {

    public ITR120ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    // Identificadores de pacote do iTR120 (baseado no documento)
    private static final int MSG_LOGIN = 0x01;
    private static final int MSG_HEARTBEAT = 0x03;
    private static final int MSG_LOCATION = 0x12;
    private static final int MSG_WARNING = 0x14;
    private static final int MSG_COMMAND = 0x80;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        // Verificar marcador inicial (0x28 0x28)
        if (buf.readUnsignedShort() != 0x2828) {
            return null;
        }

        int pid = buf.readUnsignedByte(); // PID
        int size = buf.readUnsignedShort(); // Tamanho do conteúdo
        int sequence = buf.readUnsignedShort(); // Número de sequência

        Position position = new Position(getProtocolName());
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);

        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        switch (pid) {
            case MSG_LOGIN:
                return decodeLogin(channel, buf, deviceSession);
            case MSG_HEARTBEAT:
                return decodeHeartbeat(channel, buf, position);
            case MSG_LOCATION:
                return decodeLocation(channel, buf, position);
            case MSG_COMMAND:
                return decodeCommandResponse(channel, buf, position);
            default:
                sendAck(channel, pid, sequence); // Responder ACK para pacotes não tratados
                return null;
        }
    }

    private Object decodeLogin(Channel channel, ByteBuf buf, DeviceSession deviceSession) {

        // Decodificar pacote de login (PID 0x01)
        String imei = String.format("%015d", buf.readLong()); // IMEI (8 bytes)
        buf.readUnsignedByte(); // Language
        buf.readByte(); // Timezone
        buf.readUnsignedShort(); // Sys Ver
        buf.readUnsignedShort(); // App Ver
        buf.readUnsignedShort(); // PS Ver
        buf.readUnsignedShort(); // PS OSize
        buf.readUnsignedShort(); // PS CSize
        buf.readUnsignedShort(); // PS Sum16

        // Enviar resposta de login (ACK)
        ByteBuf response = Unpooled.buffer();
        response.writeShort(0x2828); // Mark
        response.writeByte(MSG_LOGIN); // PID
        response.writeShort(0x0009); // Size
        response.writeShort(sequence); // Sequence
        response.writeInt((int) (System.currentTimeMillis() / 1000)); // Time (UTC)
        response.writeShort(0x01); // Protocol version
        response.writeByte(0x00); // PS Action
        channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));

        return null;
    }

    private Object decodeHeartbeat(Channel channel, ByteBuf buf, Position position) {

        // Decodificar pacote de heartbeat (PID 0x03)
        int status = buf.readUnsignedShort(); // Status (16 bits)
        position.set(Position.KEY_IGNITION, BitUtil.check(status, 2)); // Bit 2: ACC
        position.set(Position.KEY_STATUS, status);

        sendAck(channel, MSG_HEARTBEAT, sequence);
        return position;
    }

    private Object decodeLocation(Channel channel, ByteBuf buf, Position position) {

        // Decodificar dados de posição (PID 0x12)
        long time = buf.readUnsignedInt(); // Timestamp UNIX
        position.setTime(new Date(time * 1000));

        int mask = buf.readUnsignedByte(); // Máscara de dados válidos

        // Latitude (32 bits signed, big-endian)
        int latitudeRaw = buf.readInt();
        double latitude = latitudeRaw / 1800000.0;
        position.setLatitude(latitude);

        // Longitude (32 bits signed, big-endian)
        int longitudeRaw = buf.readInt();
        double longitude = longitudeRaw / 1800000.0;
        position.setLongitude(longitude);

        if (BitUtil.check(mask, 0)) { // GPS fixo
            position.setValid(true);
            position.setAltitude(buf.readShort()); // Altitude (metros)
            position.setSpeed(buf.readUnsignedShort()); // Velocidade (km/h)
            position.setCourse(buf.readUnsignedShort()); // Direção (graus)
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte()); // Satélites
        }

        // Status do dispositivo (16 bits)
        int status = buf.readUnsignedShort();
        position.set(Position.KEY_IGNITION, BitUtil.check(status, 2)); // Bit 2: Ignition
        position.set(Position.KEY_STATUS, status);

        sendAck(channel, MSG_LOCATION, sequence);
        return position;
    }

    private void sendAck(Channel channel, int pid, int sequence) {
        ByteBuf ack = Unpooled.buffer();
        ack.writeShort(0x2828); // Mark
        ack.writeByte(pid); // PID
        ack.writeShort(0x0002); // Size (apenas sequence)
        ack.writeShort(sequence); // Sequence
        channel.writeAndFlush(new NetworkMessage(ack, channel.remoteAddress()));
    }
}
