* Unsigned Multiply (4byte*4byte=8byte)
* Programmed by Koichi YASUOKA

* Storage for Multiplier (Given)
DATA1:	EQU	80H
* Storage for Multiplicand (Given)
DATA2:	EQU	84H
* Storage for Product (Result)
RESULT:	EQU	88H
* Storage for Partial Product (Work Area)
WORK:	EQU	0E0H
* Storage for Loop Counter (Work Area)
COUNT:	EQU	0F0H

LD	ACC,	32
	ST	ACC,	[COUNT]
	LD	IX,	4
LP1:	EOR	ACC,	ACC
	ST	ACC,	[IX+WORK-1]
	ST	ACC,	[IX+RESULT-1]
	ST	ACC,	[IX+RESULT+4-1]
	LD	ACC,	[IX+DATA2-1]
	ST	ACC,	[IX+WORK+4-1]
	SUB	IX,	1
	BNZ	LP1
LP2:	LD	ACC,	[DATA1+3]
	SRL	ACC
	LD	IX,	-4
LP3:	LD	ACC,	[IX+DATA1+4]
	RRA	ACC
	ST	ACC,	[IX+DATA1+4]
	ADD	IX,	1
	BNZ	LP3
	BNC	LP5
	LD	IX,	8
	RCF
LP4:	LD	ACC,	[IX+RESULT-1]
	ADC	ACC,	[IX+WORK-1]
	ST	ACC,	[IX+RESULT-1]
	SUB	IX,	1
	BNZ	LP4
LP5:	LD	IX,	8
	RCF
LP6:	LD	ACC,	[IX+WORK-1]
	RLA	ACC
	ST	ACC,	[IX+WORK-1]
	SUB	IX,	1
	BNZ	LP6
	LD	ACC,	[COUNT]
	SUB	ACC,	1
	ST	ACC,	[COUNT]
	BNZ	LP2
	HLT
	END
