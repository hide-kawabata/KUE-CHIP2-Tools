DATA1:	EQU	80H
DATA2:	EQU	81H
ANS:	EQU	90H
	LD	IX,	[DATA2]
	EOR	ACC,	ACC
LABEL:	ADD	ACC,	[DATA1]
	SUB	IX,	1
	BNZ	LABEL
	ST	ACC,	[ANS]
	HLT
	END
