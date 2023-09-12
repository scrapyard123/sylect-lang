public class TestD extends TestC {
	public int d() { return 4; }

	public static void main(String[] args) {
		var obj = new TestD();
		System.out.println(obj.a());
		System.out.println(obj.b());
		System.out.println(obj.c());
		System.out.println(obj.d());
	}
}
