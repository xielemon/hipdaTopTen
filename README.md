# hipdaTopTen
这是一个自动抓取D版每日十大热帖的程序
---
1.此处请用chrome抓取登录过程后填入
```java
		// USE YOUR OWN USER NAME
		map.put("username", "");
		// USE YOUR OWN USER PASSWORD, U CAN GET IT THROUGH CAPTURE POST DATA
		// WHEN U LOGIN ON CHROME
		map.put("password", "");
```

2.hipda中 
```java
		hipda test = new hipda("2016-6-24");
```
字符串即为你需要生成的哪一天的热帖
