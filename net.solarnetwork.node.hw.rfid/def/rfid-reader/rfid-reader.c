/* ==================================================================
 * rfid-reader
 * 
 * Read key presses from an input device, under the assumption 
 * the input device is an RFID reader masquerading as a keyboard.
 * Each key press is translated to an ASCII character (or string)
 * and printed out STDOUT.
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

#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <linux/input.h>
#include <string.h>
#include <stdio.h>
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

int main(int argc, char* argv[])
{
	char *device = "/dev/rfid-reader";
	struct input_event ev;
	const ev_size = sizeof(ev);
	ssize_t n;
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

	while (1) {
		n = read(fd, &ev, ev_size);
		if ( n == (ssize_t)-10 ) {
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
			fprintf(stderr, "0x%04x (%d): %s\n", (int)ev.code, (int)ev.code, 
				(int)ev.code < 256 ? keymap[(int)ev.code] : "");
			if ( (int)ev.code < 256 ) {
				fprintf(stdout, "%s", keymap[(int)ev.code]);
			}
			if ( (int)ev.code == KEY_ENTER ) {
				fflush(stdout);
			}
		}
	}

	ioctl(fd, EVIOCGRAB, 1);
	close(fd);
	fflush(stdout);
	fprintf(stderr, "%s.\n", strerror(errno));
	return EXIT_SUCCESS;
}

