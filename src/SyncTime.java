import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SyncTime {
	final static String webUrl2 = "http://www.baidu.com";
	final static String webUrl3 = "http://www.taobao.com";
	final static String webUrl4 = "http://www.ntsc.ac.cn";
	final static String webUrl5 = "http://www.360.cn";
	final static String webUrl6 = "http://www.beijing-time.org";
	final static String webUrl7 = "http://www.163.com/";
	final static String webUrl8 = "https://www.tmall.com/";
	private static boolean DEBUG = false;
	private static int iMaxDiffTimeVal = 60;
	private static int iMaxOverTimeVal = 60 * 60 * 1000;

	final static String[] webUrl = { webUrl2, webUrl3, webUrl4, webUrl5,
			webUrl6, webUrl7, webUrl8 };

	public static void main(String[] args) {
		boolean bStatus;
		DEBUG = true;
		iMaxDiffTimeVal = 1;
		iMaxOverTimeVal = 10;

		while (true) {
			bStatus = isConnect();
			if (bStatus == true) {
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		waitNetworkTimeSync();
	}

	public static boolean isConnect() {
		boolean connect = false;
		Runtime runtime = Runtime.getRuntime();
		Process process;
		int iLineNumber = 0;
		try {
			process = runtime.exec("ping " + "8.8.8.8");
			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			StringBuffer sb = new StringBuffer();
			while ((line = br.readLine()) != null) {
				sb.append(line);

				if ((line.indexOf("TTL") > 0) || (line.indexOf("ttl") > 0)) {
					connect = true;
					break;
				}
				if (iLineNumber++ > 3) {
					break;
				}
			}
			if (DEBUG) {
				System.out.println(sb.toString() + "\n\n");
			}
			is.close();
			isr.close();
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return connect;
	}

	public static long getTimeOffset(String webUrl) {
		int iRetry = 0;
		int iTimeout = 1000;
		try {
			while (iRetry++ < 5) {
				URL url = new URL(webUrl);
				URLConnection conn = url.openConnection();

				iTimeout = (iRetry + 1) * 1000;
				conn.setConnectTimeout(iTimeout);
				try {
					conn.connect();
				} catch (SocketTimeoutException e) {

				}
				long dateL = conn.getDate();
				if (dateL != 0) {
					return dateL;
				}
				if (DEBUG) {
					System.out
							.println("Retry " + webUrl + ", times: " + iRetry);
				}
			}
			return 0;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static boolean waitNetworkTimeSync() {
		long startOffset = 0;
		DateFormat dateFormatTable = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		while (true) {
			for (String url : webUrl) {
				long offset = getTimeOffset(url);
				if (offset > 0) {
					long currentOffset = System.currentTimeMillis();
					long diff = (currentOffset - offset) / 1000;
					if (Math.abs(diff) < iMaxDiffTimeVal) {
						if (DEBUG) {
							System.out.println("Time sync sucessfully");
						}
						return true;
					} else {
						Date currentDate = new Date(currentOffset);
						Date networkDate = new Date(offset);

						if (DEBUG) {
							System.out
									.println("Wait for network time sync, current: "
											+ dateFormatTable
													.format(currentDate)
											+ ", network: "
											+ dateFormatTable
													.format(networkDate));
						}
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					if (startOffset == 0) {
						startOffset = offset;
					} else {
						/* check overtime */
						if ((offset - startOffset) > iMaxOverTimeVal) {
							System.out.println("Overtime");
							return false;
						}
					}
				}
			}
		}
	}
}
