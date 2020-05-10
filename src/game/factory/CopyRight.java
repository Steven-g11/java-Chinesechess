package game.factory;

public class CopyRight {
	private static CopyRight instance = new CopyRight();
	
	private CopyRight() {}
	
	public static CopyRight getInstance() {
		return instance;
	}
	public void showMessage() {
		System.out.println("@copyright-by-fuyang and gengjunjie, SiChuanUniversity");
	}
}