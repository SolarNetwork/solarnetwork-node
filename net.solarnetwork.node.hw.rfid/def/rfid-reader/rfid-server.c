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

	if ( argc > 1 ) {
		device = argv[1];
	}

	fd = open(device, O_RDONLY);
	if ( fd  == -1 ) {
		fprintf(stderr, "Failed to open event device %s: %s.\n", device, strerror(errno));
		exit(1);
	}

	result = ioctl(fd, EVIOCGNAME(sizeof(name)), name);
	fprintf(stdout, "Reading from : %s (%s)\n", device, name);
	fflush(stdout);
	fprintf(stderr, "Getting exclusive access: ");
	result = ioctl(fd, EVIOCGRAB, 1);
	fprintf(stderr, "%s\n", (result == 0) ? "SUCCESS" : "FAILURE");

	if ( (sockfd = socket(AF_INET, SOCK_STREAM, 0)) < 0 ) {
		perror("Socket");
		exit(errno);
	}

	bzero(&self, sizeof(self));
	self.sin_family = AF_INET;
	self.sin_port = htons(kDefaultPort);
	self.sin_addr.s_addr = INADDR_ANY;

	if ( bind(sockfd, (struct sockaddr*)&self, sizeof(self)) != 0 ) {
		perror("socket--bind");
		exit(errno);
	}

	if ( listen(sockfd, 20) != 0 ) {
		perror("socket--listen");
		exit(errno);
	}

	while (1) {
		int clientfd;
		struct sockaddr_in client_addr;
		int addrlen = sizeof(client_addr);
		char buf[kBufferSize] = "";
		int bufPtr = 0;
		int len;

		// Accept incoming socket connection
		fprintf(stderr, "Accepting connections on port %d\n", kDefaultPort);
		clientfd = accept(sockfd, (struct sockaddr*)&client_addr, &addrlen);
		fprintf(stderr, "%s:%d connected\n", inet_ntoa(client_addr.sin_addr), ntohs(client_addr.sin_port));

		sprintf(buf, "Reading from: %s\n", name);
		result = send(clientfd, buf, strlen(buf), MSG_NOSIGNAL);
		if ( result < 0 ) {
			fprintf(stderr, "Error sending RFID to client: %d\n", errno);
			close(clientfd);
			continue;
		}

		while (1) {
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
			if ( ev.type == EV_KEY && ev.value == 1 ) {
				if ( (int)ev.code < kKeymapSize ) {
					len = strlen(keymap[(int)ev.code]);
					if ( len + bufPtr < kBufferSize ) {
						strncpy(&buf[bufPtr], keymap[(int)ev.code], len);
						bufPtr += len;
					}
				}
				if ( (int)ev.code == KEY_ENTER ) {
					result = send(clientfd, buf, bufPtr, MSG_NOSIGNAL); 
					bufPtr = 0;
					if ( result < 0 ) {
						fprintf(stderr, "Error sending RFID to client: %d", errno);
						break;
					}
				}
			}
		}
		close(clientfd);
	}

	close(sockfd);
	ioctl(fd, EVIOCGRAB, 1);
	close(fd);
	fflush(stdout);
	fprintf(stderr, "%s.\n", strerror(errno));
	return EXIT_SUCCESS;
}

