package hcmute.truongtrangiahung.cuoiky.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import hcmute.truongtrangiahung.cuoiky.Model.LoadingDialog;
import hcmute.truongtrangiahung.cuoiky.Model.SanPham;
import hcmute.truongtrangiahung.cuoiky.R;

public class ChiTietSanPham extends AppCompatActivity {
    // ánh xạ với View để thực hiện các sự kiện khi người dùng thao tác
    private ImageView img_Add, img_Back, img_Edit, img_HinhSanPham;
    private EditText edt_TenSanPham, edt_GiaSanPham, edt_DaBan, edt_ConLai, edt_MoTa;
    private TextView txt_MaSanPham, txt_HuyThayDoi, txt_LuuThayDoi;
    private Spinner spin_DanhMuc, spin_ThuongHieu;
    private TableLayout table_LuuThayDoi;

    // Chứa dữ liệu và hiển thị lên Spinner
    private ArrayAdapter<String> adapterThuongHieu;
    private ArrayAdapter<String> adapterDanhMuc;
    private ArrayList<String> arrayListThuongHieu = new ArrayList<>();
    private ArrayList<String> arrayListDanhMuc = new ArrayList<>();

    private SanPham sanPham = new SanPham(); // Chứa dữ liệu từ intent chuyển qua

    private int indexThuongHieu = -1; // vị trí spinner thương hiệu
    private int indexDanhMuc = -1; // vị trí spinner danh mục
    private int flagImage = 0; // Kiểm tra xem có thay đổi hình ảnh không.
    private String backgroundImageName = "null"; // lưu tên hình ảnh
    private Uri imageUri; // chứa dữ liệu khi chọn hình ảnh

    //Truy xuất dữ liệu trên Firebase Storage (Storage)
    private FirebaseStorage storage;
    private StorageReference storageReference;

    // Mã truy cập
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chi_tiet_san_pham);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        SetID();
        GetIntent();
        TaiDuLieu();
        Event();
    }

    // Lấy dữ liệu từ Firebase và lưu dữ liệu vào các biến
    private void TaiDuLieu() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("SanPham");

        String id = String.valueOf(sanPham.getId());

        Query query = myRef.orderByChild(id);
        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                final Handler handler = new Handler();
                final LoadingDialog dialog = new LoadingDialog(ChiTietSanPham.this);
                dialog.startLoadingDialog();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        String tempID = snapshot.getKey();
                        if(tempID.equals(id))
                        {
                            LoadSpinnerData();
                            sanPham = snapshot.getValue(SanPham.class);

                            txt_MaSanPham.setText(String.valueOf(sanPham.getId()));
                            edt_TenSanPham.setText(sanPham.getTenSP());
                            edt_GiaSanPham.setText(sanPham.getGia());
                            edt_DaBan.setText(String.valueOf(sanPham.getDaBan()));
                            edt_ConLai.setText(String.valueOf(sanPham.getConLai()));
                            edt_MoTa.setText(sanPham.getMoTa());
                            backgroundImageName = sanPham.getHinhAnh();
                            RetrieveImage();
                        }
                        dialog.dismissLoadingDialog();
                    }
                }, 1500);

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // Tìm vị trí trùng khới với vị trí id của sản phẩm lấy từ Firebase
    private int TimViTri(String type, int id) {
        if(type.equals("danhMuc"))
        {
            for(int i = 0; i < arrayListDanhMuc.size(); i++)
            {
                int arrayID = Integer.parseInt(arrayListDanhMuc.get(i));
                if(id == arrayID)
                    return i;
            }
        }
        else
        {
            for(int i = 0; i < arrayListThuongHieu.size(); i++)
            {
                int arrayID = Integer.parseInt(arrayListThuongHieu.get(i));
                if(id == arrayID)
                    return i;
            }
        }
        return -1;
    }

    // Lấy dữ liệu để hiển thị trong spinner
    private void LoadSpinnerData() {
        adapterDanhMuc.clear();
        adapterThuongHieu.clear();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("DanhMuc");

        myRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String danhMuc = snapshot.getValue(String.class);
                arrayListDanhMuc.add(snapshot.getKey());
                adapterDanhMuc.add(danhMuc);

                int viTriDanhMuc = TimViTri("danhMuc", sanPham.getDanhMuc());
                spin_DanhMuc.setSelection(viTriDanhMuc);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        myRef = database.getReference("ThuongHieu");
        myRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    String thuongHieu = snapshot.getValue(String.class);
                    arrayListThuongHieu.add(snapshot.getKey());
                    adapterThuongHieu.add(thuongHieu);
                    
                    int viTriThuongHieu = TimViTri("thuongHieu", sanPham.getThuongHieu());
                    spin_ThuongHieu.setSelection(viTriThuongHieu);
                    adapterThuongHieu.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        adapterDanhMuc.notifyDataSetChanged();
        adapterThuongHieu.notifyDataSetChanged();
    }

    // Lấy dữ liệu được gửi từ QuanLySanPham
    private void GetIntent() {
        Intent intent = getIntent();
        sanPham.setId( intent.getIntExtra("sanPham", -1));
    }

    // Thực thi các sự kiện khi người dùng thao tác
    private void Event() {
        img_Add.setVisibility(View.INVISIBLE);
        img_Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        img_Edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TuyChinhClick(true);
                table_LuuThayDoi.setVisibility(View.VISIBLE);
            }
        });

        txt_HuyThayDoi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CaiDatMacDinh();
            }
        });

        txt_LuuThayDoi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LuuThayDoi();
            }
        });

        spin_DanhMuc.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                indexDanhMuc = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                indexDanhMuc = 0;
            }
        });

        spin_ThuongHieu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                indexThuongHieu = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                indexThuongHieu = 0;
            }
        });

        img_HinhSanPham.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(ChiTietSanPham.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_CODE_STORAGE_PERMISSION);
                }
                else {
                    selectImage();
                }
            }
        });
    }

    //Chuyển đến Bộ sưu tập của máy và chọn hình
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if(intent.resolveActivity(getPackageManager()) != null)
        {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }

    // Cài đặt dữ liệu mặc định
    private void CaiDatMacDinh() {
       TuyChinhClick(false);

        table_LuuThayDoi.setVisibility(View.INVISIBLE);

        txt_MaSanPham.setText(String.valueOf(sanPham.getId()));
        edt_TenSanPham.setText(sanPham.getTenSP());
        edt_GiaSanPham.setText(sanPham.getGia());
        edt_DaBan.setText(String.valueOf(sanPham.getDaBan()));
        edt_ConLai.setText(String.valueOf(sanPham.getConLai()));
        edt_MoTa.setText(sanPham.getMoTa());
        int viTriDanhMuc = TimViTri("danhMuc", sanPham.getDanhMuc());
        spin_DanhMuc.setSelection(viTriDanhMuc);
        int viTriThuongHieu = TimViTri("thuongHieu", sanPham.getThuongHieu());
        spin_ThuongHieu.setSelection(viTriThuongHieu);
    }

    // Lưu dữ liệu đã chỉnh sửa và cập nhật lên Firebase
    private void LuuThayDoi() {
        //Lấy id danh mục
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("SanPham/"+sanPham.getId());

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //Thêm dữ liệu

                System.out.println("DanhMuc: " + arrayListDanhMuc.size());
                System.out.println("IndexDM: " + indexDanhMuc);
                int danhMuc = Integer.parseInt(arrayListDanhMuc.get(indexDanhMuc));
                int thuongHieu = Integer.parseInt(arrayListThuongHieu.get(indexThuongHieu));

                SanPham temp = new SanPham(sanPham.getId(), edt_TenSanPham.getText().toString(),danhMuc, thuongHieu,
                        edt_GiaSanPham.getText().toString(), Integer.parseInt(edt_DaBan.getText().toString()), Integer.parseInt(edt_ConLai.getText().toString()),
                        edt_MoTa.getText().toString(), backgroundImageName);

                myRef.setValue(temp, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                        Toast.makeText(ChiTietSanPham.this, "Đã lưu", Toast.LENGTH_SHORT).show();
                        TuyChinhClick(false);
                        table_LuuThayDoi.setVisibility(View.INVISIBLE);
                        TaiDuLieu();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        if(flagImage == 1)
        {
            uploadImage();
        }
    }

    // Gán id vào các biến và cài đặt adapter
    private void SetID() {
        img_Add = findViewById(R.id.img_Add);
        img_Back = findViewById(R.id.img_Back);
        img_Edit = findViewById(R.id.img_Edit);
        img_HinhSanPham = findViewById(R.id.img_HinhSanPham);
        table_LuuThayDoi = findViewById(R.id.table_LuuThayDoi);
        edt_TenSanPham = findViewById(R.id.edt_TenSanPham);
        txt_MaSanPham = findViewById(R.id.txt_MaSanPham);
        edt_GiaSanPham = findViewById(R.id.edt_GiaSanPham);
        edt_ConLai = findViewById(R.id.edt_SoLuongSanPhamConLai);
        edt_DaBan = findViewById(R.id.edt_SoLuongSanPhamDaBan);
        edt_MoTa = findViewById(R.id.edt_MoTaSanPham);
        txt_HuyThayDoi = findViewById(R.id.txt_HuyThayDoi);
        txt_LuuThayDoi = findViewById(R.id.txt_LuuThayDoi);
        spin_DanhMuc = findViewById(R.id.spin_DanhMucSanPham);
        spin_ThuongHieu = findViewById(R.id.spin_ThuongHieuSanPham);

        TuyChinhClick(false);

        adapterDanhMuc = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        adapterThuongHieu = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        spin_DanhMuc.setAdapter(adapterDanhMuc);
        spin_ThuongHieu.setAdapter(adapterThuongHieu);
    }

    // Dùng để chỉnh các view có thao tác được hay không
    private void TuyChinhClick(boolean b) {
        edt_TenSanPham.setEnabled(b);
        edt_GiaSanPham.setEnabled(b);
        edt_MoTa.setEnabled(b);
        edt_DaBan.setEnabled(b);
        edt_ConLai.setEnabled(b);
        img_HinhSanPham.setEnabled(b);
        spin_ThuongHieu.setEnabled(b);
        spin_DanhMuc.setEnabled(b);
    }

    // Kiểm tra quyền truy cập
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0)
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                selectImage();
            }
            else {
                Toast.makeText(ChiTietSanPham.this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Lấy kết quả trả về khi chọn ảnh và lưu vào các biến, view
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
            if(data != null){
                imageUri = data.getData();
                if(imageUri != null){
                    img_HinhSanPham.setImageURI(imageUri);
                    backgroundImageName = getFileName(imageUri, getApplicationContext());
                    flagImage = 1;
                }
            }
        }
    }

    // Tải hình ảnh lên Storage
    private void uploadImage() {
        final String randomKey = backgroundImageName;
        StorageReference riverRef = storageReference.child("Image/ " + randomKey);

        riverRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {

                    }
                });
    }

    // Lấy tên file của hình ảnh đã chọn
    private String getFileName (Uri uri, Context context){
        String res = null;
        if(uri.getScheme().equals("content")){
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null );
            try {
                if(cursor != null && cursor.moveToFirst()){
                    res = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

                }
            }finally {
                cursor.close();
            }
            if (res == null){
                res = uri.getPath();
                int cutt = res.lastIndexOf('/');
                if (cutt != -1){
                    res = res.substring(cutt+1);
                }
            }
        }
        return res;
    }

    // Lấy hình ảnh từ Storage theo tên được lưu trong backgroundImageName
    public void RetrieveImage(){
        String name  = backgroundImageName;
        StorageReference storageReference = FirebaseStorage.getInstance().getReference("Image/ " + name);
        try {
            File localFile = File.createTempFile("tempfile", ".jpg");
            storageReference.getFile(localFile)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                            img_HinhSanPham.setImageBitmap(bitmap);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}