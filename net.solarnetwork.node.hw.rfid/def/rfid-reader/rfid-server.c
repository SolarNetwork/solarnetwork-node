/* ==================================================================
 * rfid-server
 * 
 * Listen on a socket, and once connected read key presses from an 
 * input device, under the assumption  the input device is an RFID 
 * reader masquerading as a keyboard. Each key press is translated
 * to an ASCII character (or string) and sent to client. Each RFID
 * ID is assumed to be terminated by an ENTER key (newline).
 *
 * While the server is waiting for RFID input, it will on occasion
 * send a "heartbeat" message to the client, to ensure the
 * connection is still valid. The message is the string "ping"
 * followed by a newline character.
 * 
 * The server handles only a single connection at a time!
 *
 * Pass the device name to read from as the program agrument.
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

#include <arpa/inet.h>
#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <linux/input.h>
#include <resolv.h>
#include <string.h>
#include <stdio.h>
#include <sys/socket.h>
#include <unistd.h>

static const char *const keymap[256] = {
	"", "<esc>", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
	"-", "=", "<backspace>",
	"<tab>", "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "[",
	"]", "\n", "<control>", "a", "s", "d", "f", "g",
	"h", "j", "k", "l", ";", "'", "", "<shift>",
	"\\", "z", "x", "c", "v", "b", "n", "m", ",", ".",
	"/", "<shift>", "", "<alt>", " ", "<capslock>", "<f1>",
	"<f2>", "<f3>", "<f4>", "<f5>", "<f6>", "<f7>", "<f8>", "<f9>",
	"<f10>", "<numlock>", "<scrolllock>", "", "", "", "", "", "", "",
	"", "", "", "\\", "f11", "f12", "", "", "", "", "", "",
	"", "", "<control>", "", "<sysrq>"
};

#define kBufferSize 1024

static const int kKeymapSize = 256;
static const int kDefaultPort = 9090;
static const int kDefaultTimeoutSecs = 10;

int main(int argc, char* argv[])
{
	char *device = "/dev/rfid-reader";
	struct input_event ev;
	const ev_size = sizeof(ev);
	ssize_t n;
	int sockfd;
	struct sockaddr_in self;
	int fd;
	int result = 0;
	char name[256] = "Unknown";
	char heartbeat[] = "ping\n";
	int on = 0;

	if ( argc > 1 ) {
		// get the device to read from the first arg
		device = argv[1];
	}

	// open our RFID input device
	fd = open(device, O_RDONLY);
	if ( fd  == -1 ) {
		fprintf(stderr, "Failed to open event device %s: %s.\n", device, strerror(errno));
		exit(1);
	}

	// read the device name
	result = ioctl(fd, EVIOCGNAME(sizeof(name)), name);
	fprintf(stdout, "Reading from : %s (%s)\n", device, name);
	fflush(stdout);
	
	// get exclusive access to the device, so that RFID keyboard events don't go to the system console
	fprintf(stderr, "Getting exclusive access: ");
	result = ioctl(fd, EVIOCGRAB, 1);
	fprintf(stderr, "%s\n", (result == 0) ? "SUCCESS" : "FAILURE");

	// create a INET socket to serve up the RFID IDs
	if ( (sockfd = socket(AF_INET, SOCK_STREAM, 0)) < 0 ) {
		perror("Socket");
		exit(errno);
	}

	// set the SO_REUSEADDR option so that we can immediately re-claim this socket
	// if the program quits and restarts before the TCP TIME_WAIT timeout happens
	on = 1;
	result = setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, (const char *) &on, sizeof(on));
	if ( result < 0 ) {
		perror("setsockopt-reuseaddr");
	}

	// set up the port # we'd like to listen on and bind to any address
	bzero(&self, sizeof(self));
	self.sin_family = AF_INET;
	self.sin_port = htons(kDefaultPort);
	self.sin_addr.s_addr = INADDR_ANY;
	if ( bind(sockfd, (struct sockaddr*)&self, sizeof(self)) != 0 ) {
		perror("socket--bind");
		exit(errno);
	}

	// start listening on our new socket for a connection
	if ( listen(sockfd, 20) != 0 ) {
		perror("socket--listen");
		exit(errno);
	}

	// loop forever, waiting for a socket connection
	while (1) {
		int clientfd;
		struct sockaddr_in client_addr;
		int addrlen = sizeof(client_addr);
		char buf[kBufferSize] = "";
		int bufPtr = 0;
		int len;
		fd_set set;
  		struct timeval timeout;

		// accept incoming socket connection
		fprintf(stderr, "Accepting connections on port %d\n", kDefaultPort);
		clientfd = accept(sockfd, (struct sockaddr*)&client_addr, &addrlen);
		fprintf(stderr, "%s:%d connected\n", inet_ntoa(client_addr.sin_addr), ntohs(client_addr.sin_port));

		// send a preamble to the client, in the form of a status message about what device we're 
		// reading from, terminated by a newline
		sprintf(buf, "Reading from: %s\n", name);
		result = send(clientfd, buf, strlen(buf), MSG_NOSIGNAL);
		if ( result < 0 ) {
			fprintf(stderr, "Error sending preamble to client: %d\n", errno);
			close(clientfd);
			continue;
		}

		// we're connected to a client now, loop forever waiting for RFID keyboard events
		while (1) {
			// set a timeout on our read from the RFID device, so we can periodically test
			// that the socket connection is still alive via "heartbeat" messages
			timeout.tv_sec = kDefaultTimeoutSecs;
			timeout.tv_usec = 0;
			FD_ZERO(&set);
			FD_SET(fd, &set);
			n = select(fd + 1, &set, NULL, NULL, &timeout);
			if ( n == (ssize_t)-1 ) {
				perror("select");
				break;
			} else if ( n == (ssize_t)0 ) {
				// read timeout from RFID device; ping server with heartbeat				
				result = send(clientfd, heartbeat, strlen(heartbeat), MSG_NOSIGNAL);
				bufPtr = 0;
				if ( result < 0 ) {
					fprintf(stderr, "Error sending heartbeat to client: %d\n", errno);
					
					// exit out of RFID read loop, to jump back to accepting connection loop
					break;
				}
				
				// continue to wait for more RFID data
				continue;
			}
			
			// we've got some data, so read it into our input event structure
			n = read(fd, &ev, ev_size);
			if ( n == (ssize_t)-1 ) {
				if ( errno == EINTR ) {
					continue;
				} else {
					break;
				}
			} else if ( n != ev_size ) {
				errno = EIO;
				break;
			}
			
			// copy each keyboard input event into a buffer until we find a KEY_ENTER (newline)
			if ( ev.type == EV_KEY && ev.value == 1 ) {
				if ( (int)ev.code < kKeymapSize ) {
					len = strlen(keymap[(int)ev.code]);
					if ( len + bufPtr < kBufferSize ) {
						strncpy(&buf[bufPtr], keymap[(int)ev.code], len);
						bufPtr += len;
					}
				}
				if ( (int)ev.code == KEY_ENTER ) {
					// we found our newline key press, so send our collected RFID ID value to the client
					result = send(clientfd, buf, bufPtr, MSG_NOSIGNAL);
					
					// reset our buffer position back to 0
					bufPtr = 0;
					if ( result < 0 ) {
						fprintf(stderr, "Error sending RFID to client: %d", errno);
						break;
					}
				}
			}
		}
		
		// done sending to client, close the connection and start listening again
		close(clientfd);
	}

	close(sockfd);
	ioctl(fd, EVIOCGRAB, 1);
	close(fd);
	fflush(stdout);
	fprintf(stderr, "%s.\n", strerror(errno));
	return EXIT_SUCCESS;
}
