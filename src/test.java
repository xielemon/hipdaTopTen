import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test
{
  public static void main(String[] args)
  {
    String txt="<a href=\"logging.php?action=logout&amp;formhash=18666db3\">ÍË³ö</a>";


    Pattern p = Pattern.compile("formhash=\\w{0,10}");
    Matcher m = p.matcher(txt);
	System.out.println(m.toString());
    if (m.find())
    {
        String var1=m.group();
        System.out.print("("+var1.toString()+")"+"\n");
    }
  }
}