* Calculate CRC(Cyclic Redundancy Check) Code
* Programmed by Akira Uejima

* Data on data page
DATA:	EQU	00H
* Data Length(byte) on program page
	N:	EQU	80H
* Resultant CRC on program page
C1:	EQU	0C0H
C2:	EQU	0C1H
* Work Area on program page
WORK:	EQU	0F0H

	LD	ACC,	0FFH
	ST	ACC,	[C1]
	ST	ACC,	[C2]
	EOR	IX,	IX
	ST	IX,	[WORK]
LP1:	LD	ACC,	[C1]
	EOR	ACC,	(IX+DATA)
	ST	ACC,	[C1]
	LD	IX,	8
LP2:	LD	ACC,	[C2]
	SLL	ACC
	ST	ACC,	[C2]
	LD	ACC,	[C1]
	RLA	ACC
	ST	ACC,	[C1]
	BNC	SKIP
	EOR	ACC,	[P1]
	ST	ACC,	[C1]
	LD	ACC,	[C2]
	EOR	ACC,	[P2]
	ST	ACC,	[C2]
SKIP:	SUB	IX,	1
	BP	LP2
	LD	IX,	[WORK]
	ADD	IX,	1
	CMP	IX,	[N]
	ST	IX,	[WORK]
	BNZ	LP1
	LD	ACC,	[C2]
	EOR	ACC,	0FFH
	ST	ACC,	[C2]
	LD	ACC,	[C1]
	EOR	ACC,	0FFH
	ST	ACC,	[C1]
	HLT

* CRC Generator Polynomial on program page
* CCITT(x^16 + x^12 + x^5 + 1) -> 1 0001 0000 0010 0001
*                                     1    0    2    1
P1:	PROG	10H
P2:	PROG	21H

	END
