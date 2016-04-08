package com.coolweather.app.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.app.R;
import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.County;
import com.coolweather.app.model.Province;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

public class ChooseAreaActivity extends Activity {

	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;

	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;

	/**
	 * ������ʱ���һЩʡ���С��ص����ݵ�
	 */
	private List<String> dataList = new ArrayList<String>();

	/**
	 * ʡ�б�
	 */
	private List<Province> provinceList;
	/**
	 * ���б�
	 */
	private List<City> cityList;
	/**
	 * ���б�
	 */
	private List<County> countyList;
	/**
	 * ѡ�е�ʡ��
	 */
	private Province selectedProvince;
	/**
	 * ѡ�е���
	 */
	private City selectedCity;
	/**
	 * ��ǰѡ�еļ���
	 */
	private int currentLevel;
	
	/**
	 * �Ƿ��WeatherActivity����ת����
	 */
	private boolean isFromWeatherActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		/**
		 * һ���򿪸ó�����ĳ�������Ѿ�ѡ�У��ͻ�ֱ����ת������ҳ��;
		 * ����Ѿ�ѡ���˳��в��Ҳ��Ǵ�WeatherActivity��ת�������Ż�ֱ����ת��WeatherActivity
		 */
		if (prefs.getBoolean("city_selected", false) && !isFromWeatherActivity) {
			Intent intent = new Intent(this, WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView = (ListView) findViewById(R.id.list_view);
		titleText = (TextView) findViewById(R.id.title_text);
		// ����dataList��ÿһ�εĵ���¼��У�dataList��ֵ����ı䣬
		// Ȼ��ͨ������notifyDataSetChanged()�����ı�ListView�е�����(��ʾ)��
		// �𵽵ݹ���ʾ��ͬ�����ʡ�������ݵ�����
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, dataList);
		listView.setAdapter(adapter);
		coolWeatherDB = CoolWeatherDB.getInstance(this);
		// �ݹ���ListView��ĳ��Item���ͻ�۲쵽��������ʾ�����ݲ����仯����TextView��ListView��
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (currentLevel == LEVEL_PROVINCE) {
					// ���ĳ��item�ͱ�ʾѡ�����Ǹ�ʡ����λ�����Ĵ���
					selectedProvince = provinceList.get(position);
					// Ȼ�����תҳ�棬��ѯ������Ϣ
					queryCities();// �������й��ܣ�notifyDataSetChanged()
				} else if (currentLevel == LEVEL_CITY) {
					selectedCity = cityList.get(position);
					queryCounties();// �������й��ܣ�notifyDataSetChanged()
				} else if (currentLevel == LEVEL_COUNTY) {
					String countyCode = countyList.get(position)
							.getCountyCode();
					Intent intent = new Intent(ChooseAreaActivity.this,
							WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();//����ת������ҳ���ʱ��͹رյ�ǰҳ��
				}
			}
		});
		// �ȼ���ʡ������
		queryProvinces();// �������й��ܣ�notifyDataSetChanged()
	}

	/**
	 * ��ѯȫ�����е�ʡ�����ȴ����ݿ��в�ѯ��������ݿ���û�о�����(�ӷ�����)��ѯ
	 */
	private void queryProvinces() {
		provinceList = coolWeatherDB.loadProvinces();
		if (provinceList.size() > 0) {
			dataList.clear();
			for (Province province : provinceList) {
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("�й�");
			currentLevel = LEVEL_PROVINCE;
		} else {
			queryFromServer(null, "province");
		}

	}

	/**
	 * ���ݴ���Ĵ��ź����ʹӷ������������ݣ�ʡ���أ�
	 * 
	 * @param code
	 *            ����Ĵ��ű�ʾ��ǰ�ĵؼ���λ����һ�������id
	 * @param type
	 *            ָ����ѯ����ʡ �����У��������
	 */
	private void queryFromServer(final String code, final String type) {
		String address;
		if (!TextUtils.isEmpty(code)) {
			address = "http://www.weather.com.cn/data/list3/city" + code
					+ ".xml";
		} else {
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {

			@Override
			public void onError(Exception e) {
				// ͨ��runOnUiThread()�����ص����̴߳����߼�
				runOnUiThread(new Runnable() {
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "����ʧ��",
								Toast.LENGTH_SHORT).show();
					}

				});
			}

			@Override
			public void OnFinished(String response) {
				boolean result = false;
				if ("province".equals(type)) {
					// �����������ص����ݴ洢�����ݿ��Province����
					result = Utility.handleProvincesResponse(coolWeatherDB,
							response);
				} else if ("city".equals(type)) {
					// �����������ص����ݴ洢�����ݿ��City����
					result = Utility.handleCitiesResponse(coolWeatherDB,
							response, selectedProvince.getId());
				} else if ("county".equals(type)) {
					// �����������ص����ݴ洢�����ݿ��County����
					result = Utility.handleCountiesResponse(coolWeatherDB,
							response, selectedCity.getId());
				}
				if (result) {
					// ͨ��runOnUiThread()�����ص����̴߳����߼�
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							closeProgressDialog();
							if ("province".equals(type)) {
								queryProvinces();
							} else if ("city".equals(type)) {
								queryCities();
							} else if ("county".equals(type)) {
								queryCounties();
							}
						}
					});
				}
			}
		});
	}

	/**
	 * ��ʾ���ȶԻ���
	 */
	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("���ڼ���...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}

	/**
	 * �رս��ȶԻ���
	 */
	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}

	/**
	 * ��ѯȫʡ���е��У����ȴ����ݿ��в�ѯ��������ݿ���û�о�����(�ӷ�����)��ѯ
	 */
	protected void queryCities() {
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());
		if (cityList.size() > 0) {
			// ����dataList��ʱ�洢���е���Ϣ
			dataList.clear();
			for (City city : cityList) {
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		} else {
			queryFromServer(selectedProvince.getProvinceCode(), "city");
		}
	}

	/**
	 * ��ѯȫʡ���е��أ����ȴ����ݿ��в�ѯ��������ݿ���û�о�����(�ӷ�����)��ѯ
	 */
	private void queryCounties() {
		countyList = coolWeatherDB.loadCounties(selectedCity.getId());
		if (countyList.size() > 0) {
			dataList.clear();
			for (County county : countyList) {
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		} else {
			queryFromServer(selectedCity.getCityCode(), "county");
		}
	}

	/**
	 * ���˵�ʱ����ݵ�ǰ������ʾ����ʡ���С��صļ����˻ص���һ����λ
	 */
	@Override
	public void onBackPressed() {
		if (currentLevel == LEVEL_COUNTY) {
			queryCities();
		} else if (currentLevel == LEVEL_CITY) {
			queryProvinces();
		} else {
			//���»ص�ԭ����WeatherActivity
			if (isFromWeatherActivity) {
				Intent intent = new Intent(this,WeatherActivity.class);
				startActivity(intent);
			}
			finish();
		}
	}

}
