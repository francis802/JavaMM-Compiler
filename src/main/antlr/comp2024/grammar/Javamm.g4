grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;


CLASS : 'class' ;
INT : 'int' ;
DOUBLE : 'double' ;
FLOAT : 'float' ;
BOOLEAN : 'boolean' ;
STRING : 'String' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

INTEGER : [0] | ([1-9][0-9]*) ;
ID : [a-zA-Z]([a-zA-Z_0-9])* ;

WS : [ \t\n\r\f]+ -> skip ;
SCOMMENT : '//' .*? '\n' -> skip;
MCOMMENT : '/*' .*? '*/' -> skip;


program
    : (importDecl)* classDecl EOF
    ;


importDecl
    : 'import' name=ID ('.' name=ID)* SEMI #ImportStmt
    ;

classDecl
    : CLASS classname=ID ('extends' extendedname=ID)? LCURLY varDecl* methodDecl* RCURLY;

varDecl
    : type name=ID SEMI;

type
    : type '[' ']'
    | name= INT
    | name= DOUBLE
    | name= FLOAT
    | name= BOOLEAN
    | name= STRING
    | 'int' '...'
    | name = ID
    ;


methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})? type name=ID LPAREN param? RPAREN LCURLY varDecl* stmt* RCURLY
    | (PUBLIC {$isPublic=true;})? 'static' 'void' 'main'LPAREN STRING '[' ']' name=ID RPAREN LCURLY (varDecl)* (stmt)* RCURLY
    ;


param
    : type name=ID (',' param)*
    ;


stmt
    : expr SEMI #ExprSmt //
    | LCURLY (stmt)* RCURLY #CurlyStmt //
    | RETURN expr SEMI #ReturnStmt //
    | ifExpr (elseIfExpr)* (elseExpr)? #ConditionalStmt //
    | 'for' LPAREN stmt expr ';' expr RPAREN stmt #ForStmt //
    | 'while' LPAREN expr RPAREN stmt #WhileStmt //
    | expr EQUALS expr SEMI #AssignStmt //
    ;


ifExpr
    : 'if' LPAREN expr RPAREN stmt;

elseIfExpr
    : 'else if' LPAREN expr RPAREN stmt;

elseExpr
    : 'else' stmt;


expr
    : LPAREN expr RPAREN #Parenthesis //
    | expr '[' expr ']' #ArraySubs //
    | 'new' type '[' expr ']' #ArrayDeclaration //
    | 'new' name=ID LPAREN (expr (',' expr)*)? RPAREN #ObjectDeclaration //
    | expr '.' name=ID LPAREN (expr (',' expr)*)? RPAREN #FunctionCall //
    | expr op= (MUL | DIV) expr #BinaryExpr //
    | expr op= (ADD | SUB) expr #BinaryExpr //
    | expr op=('*=' | '/=' | '*=' | '-=') expr #BinaryExpr //
    | expr op=('<' | '>' | '<=' | '>=' | '==' | '!=') expr #BinaryExpr //
    | expr op='&&' expr #BinaryExpr //
    | expr op='||' expr #BinaryExpr //
    | value=INTEGER #IntegerLiteral //
    | value='true' #TrueLiteral //
    | value='false' #FalseLiteral //
    | name=ID #VarRefExpr //
    | value = 'this' #Object //
    | value = '!' expr #Negation //
    | expr '.' 'length' #Length //
    | '[' ( expr ( ',' expr )* )? ']' #DescribedArray
    ;



