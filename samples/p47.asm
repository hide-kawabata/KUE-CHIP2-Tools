* Marching Memory Test
* Programmed by Hiroyuki OCHI

* Rn with PC=prog
* Tests program page [test] through [prog-1] and data page (0) through (0ffh).
* Blinks OBUF LEDs while no error is detected.
* Halts when an error is detected.

* Original Address of Program
PROG:	EQU	0

CA:	EQU	PROG

	EOR	ACC,	ACC
	LD	IX,	TEST
LP0:	ST	ACC,	[IX]
	ADD	IX,	1
	CMP	IX,	PROG
	BNZ	LP0
	EOR	IX,	IX
LP1:	ST	ACC,	(IX)
	ADD	IX,	1
	BNZ	LP1
LP2:	LD	IX,	TEST
LP3:	CMP	ACC,	[IX]
	BNZ	ERR
	EOR	ACC,	80H
	RLL	ACC
	ST	ACC,	[IX]
	BZ	NX3
	CMP	ACC,	0FFH
	BNZ	LP3
NX3:	EOR	ACC,	0FFH
	ADD	IX,	1
	CMP	IX,	PROG
	BNZ	LP3
	EOR	IX,	IX
LP4:	CMP	ACC,	(IX)
	BNZ	ERR
	EOR	ACC,	80H
	RLL	ACC
	ST	ACC,	(IX)
	BZ	NX4
	CMP	ACC,	0FFH
	BNZ	LP4
NX4:	EOR	ACC,	0FFH
	ADD	IX,	1
	BNZ	LP4
	EOR	ACC,	0FFH
	OUT
	BA	LP2
ERR:	HLT	

TEST:	EQU	CA
	END
