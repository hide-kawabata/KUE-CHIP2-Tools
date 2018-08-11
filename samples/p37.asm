* Bubble Sorting
* Programmed by Akira Uejima

* Data(signed) on data page
DATA:	EQU	00H
* Data Length(byte) on program page
N:	EQU	80H
* Work Area(loop counter) on program page
WORK1:	EQU	90H
* Work Area(swap area) on program page
WORK2:	EQU	91H

	LD	IX,	[N]
	SUB	IX,	1
	ST	IX,	[WORK1]
LP1:	EOR	IX,	IX
	RCF
LP2:	LD	ACC,	(IX+DATA)
	CMP	ACC,	(IX+DATA+1)
	BLE	SKIP
	ST	ACC,	[WORK2]
	LD	ACC,	(IX+DATA+1)
	ST	ACC,	(IX+DATA)
	LD	ACC,	[WORK2]
	ST	ACC,	(IX+DATA+1)
	SCF
SKIP:	ADD	IX,	1
	CMP	IX,	[WORK1]
	BNZ	LP2
	BNC	FIN
	LD	IX,	[WORK1]
	SUB	IX,	1
	ST	IX,	[WORK1]
	BNZ	LP1
FIN:	HLT

	END
