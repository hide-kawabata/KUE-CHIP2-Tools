* Unsigned Multiply (1byte*1byte=2byte)
* Programmed by A.Uejima

* Multiplier
DATA1:	EQU	80H
* Multiplicand
DATA2:	EQU	81H
* Product
ANS:	EQU	82H
* Work Area
WORK:	EQU	0F0H
	EOR	ACC,	ACC
	ST	ACC,	[ANS]
	ST	ACC,	[ANS+1]
	ST	ACC,	[WORK]
	LD	ACC,	[DATA2]
	ST	ACC,	[WORK+1]
	LD	IX,	[DATA1]
LOOP:	SRL	IX
	BNC	SKIP
	RCF
	LD	ACC,	[ANS+1]
	ADC	ACC,	[WORK+1]
	ST	ACC,	[ANS+1]
	LD	ACC,	[ANS]
	ADC	ACC,	[WORK]
	ST	ACC,	[ANS]
SKIP:	CMP	IX,	0
	BZ	FIN
	LD	ACC,	[WORK+1]
	SLA	ACC
	ST	ACC,	[WORK+1]
	LD	ACC,	[WORK]
	RLA	ACC
	ST	ACC,	[WORK]
	BA	LOOP
FIN:	HLT

	END
