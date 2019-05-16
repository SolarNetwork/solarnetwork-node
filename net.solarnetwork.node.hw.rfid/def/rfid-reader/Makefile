BDIR = build
ODIR = $(BDIR)/obj
PDIR = $(BDIR)/bin
UDIR = $(BDIR)/system
SDIR = src

all : dirs $(PDIR)/sn-rfid-reader $(PDIR)/sn-rfid-server \
	$(UDIR)/sn-rfid-server@.service

dirs : $(ODIR) $(PDIR) $(UDIR)

$(ODIR) :
	mkdir -p $(ODIR)

$(PDIR) :
	mkdir -p $(PDIR)

$(UDIR) :
	mkdir -p $(UDIR)

$(PDIR)/sn-rfid-reader : $(ODIR)/rfid-reader.o
	cc -o $(PDIR)/sn-rfid-reader $(ODIR)/rfid-reader.o

$(ODIR)/rfid-reader.o : $(SDIR)/rfid-reader.c
	cc -c $(SDIR)/rfid-reader.c -o $(ODIR)/rfid-reader.o

$(PDIR)/sn-rfid-server : $(ODIR)/rfid-server.o
	cc -o $(PDIR)/sn-rfid-server $(ODIR)/rfid-server.o

$(ODIR)/rfid-server.o : $(SDIR)/rfid-server.c
	cc -c $(SDIR)/rfid-server.c -o $(ODIR)/rfid-server.o

$(UDIR)/sn-rfid-server@.service :
	cp sn-rfid-server@.service $(UDIR)

clean :
	rm $(PDIR)/sn-rfid-server $(ODIR)/rfid-server.o \
		$(PDIR)/sn-rfid-reader $(ODIR)/rfid-reader.o \
		$(UDIR)/sn-rfid-server@.service
