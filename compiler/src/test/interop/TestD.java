public class TestD extends TestC {
	public int a() { return super.a() + 1; }
	public int b() { return super.b() + 1; }
	public int c() { return super.c() + 1; }
	public int d() { return 4; }

	public static void main(String[] args) {
		var obj = new TestD();
		System.out.println(obj.a());
		System.out.println(obj.b());
		System.out.println(obj.c());
		System.out.println(obj.d());
	}
}
