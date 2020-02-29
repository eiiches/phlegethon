import com.google.errorprone.annotations.Var;

public class Test {
	public static void main(String args[]) throws Exception {
		@Var Long a = null;
		for (long i = 0; i < Long.MAX_VALUE; ++i)
			a = i;
		System.out.println(a);
	}
}
