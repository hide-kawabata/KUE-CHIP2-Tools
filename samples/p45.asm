* Unsigned Division (1byte/1byte=1byte...1byte)
* Programmed by A.Uejima

* Dividend
DVD:	EQU	80H
* Divisor (!= 0)
DVS:	EQU	81H
* Quotient
QOT:	EQU	82H
* Remainder
RMD:	EQU	83H
* Work Area
WORK:	EQU	0F0H

	EOR	ACC,	ACC
	ST	ACC,	[QOT]
	ST	ACC,	[RMD]
	LD	ACC,	[DVD]
	ST	ACC,	[WORK]
	LD	IX,	8
LOOP:	LD	ACC,	[WORK]
	SLA	ACC
	ST	ACC,	[WORK]
	LD	ACC,	[RMD]
	RLA	ACC
	RCF
	SBC	ACC,	[DVS]
	BC	SP1
	SCF
	BA	SP2
SP1:	ADD	ACC,	[DVS]
	RCF
SP2:	ST	ACC,	[RMD]
	LD	ACC,	[QOT]
	RLA	ACC
	ST	ACC,	[QOT]
	SUB	IX,	1
	BP	LOOP
	HLT

	END
