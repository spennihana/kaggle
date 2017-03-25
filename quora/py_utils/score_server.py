import asyncore
import socket
import struct

class EchoHandler(asyncore.dispatcher_with_send):

    def handle_read(self):
        try:
            data = self.recv(8192)
            print "{} bytes received".format(len(data))
            data = struct.unpack("d", data)
            print "received data {}".format(data)
            if data:
                self.send(bytearray(struct.pack("d", 1.111111)))
        except:
            print "Client disconnect..."

class EchoServer(asyncore.dispatcher):

    def __init__(self, host, port):
        asyncore.dispatcher.__init__(self)
        self.create_socket(socket.AF_INET, socket.SOCK_STREAM)
        self.set_reuse_addr()
        self.bind((host, port))
        self.listen(5)

    def handle_accept(self):
        pair = self.accept()
        if pair is not None:
            sock, addr = pair
            print 'Incoming connection from %s' % repr(addr)
            handler = EchoHandler(sock)

server = EchoServer('localhost', 34534)
asyncore.loop()
