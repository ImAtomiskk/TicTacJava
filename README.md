# TicTacJava
## A Java version of TicTacToe (ignore the pun)

### 1.0 - How to Play
**Upon Launching TicTacJava with no arguments, You'll See a 3x3 grid like the one shown below**
```
  0   1   2
0   |   |  
 ----------
1   |   |  
 ----------
2   |   |  

```
**Below that, you'll see a bit of text**
```
 Player X's turn
 Enter Row # (0-2) 
```
**Enter The Corresponding Number (example 0, 1, or 2) row tab**
```
     0   1   2
-> 0   |   |   
     ----------
-> 1   |   |   
     ----------
-> 2   |   |   

```
**Then Enter the Column Number (0,1,2)**
```
 -> 0   1   2 <-
  0   |   |  
    ----------
  1   |   |  
    ----------
  2   |   |  

```

### 1.1 - Botplay
**To Launch TicTacJava with Botplay, Enter the Following**
```
java -jar TicTacJava.jar --botplay
```
**To Launch with a specific difficulty, Enter one of the Following**
```
# Easy
java -jar TicTacJava.jar --botplay --easy

# Normal
java -jar TicTacJava.jar --botplay
OR
java -jar TicTacJava.jar --botplay --normal

# Hard
java -jar TicTacJava.jar --botplay --hard
```

### 1.2 - Size
**To Change the Size, Enter the following**
```
java -jar TicTacJava.jar --size <number>
```

### 1.3 - Starting Character
**If you want to change you character from X to O, enter the following**
```
java -jar TicTacJava.jar --startplayer O
```
or the opposite way around
```
java -jar TicTacJava.jar --startplayer X
```

### 1.4 - Color
**To change you color for the characters, grid, etc, enter the following (see below for colors)**  
X Color Change
```
java -jar TicTacJava.jar --color-x <color>
```
O Color Change
```
java -jar TicTacJava.jar --color-o <color>
```
Grid Color Change
```
java -jar TicTacJava.jar --color-grid <color>
```
**Available Colors**  
<font color="brightred">RED</font>  
<font color="green">GREEN</font>  
<font color="yellow">YELLOW</font>  
<font color="blue">BLUE</font>  
<font color="purple">PURPLE</font>  
<font color="cyan">CYAN</font>  
WHITE  
<font color="red">BOLD_RED</font>  
<font color="lime">BOLD_GREEN</font>  
<font color="gold">BOLD_YELLOW</font>  
<font color="lightblue">BOLD_BLUE</font>  
<font color="lightcyan">BOLD_CYAN</font> 

### 1.5 - Server Hosting