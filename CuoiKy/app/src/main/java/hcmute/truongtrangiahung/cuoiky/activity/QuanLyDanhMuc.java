package hcmute.truongtrangiahung.cuoiky.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import hcmute.truongtrangiahung.cuoiky.Adapter.ItemChiTietQuanLyAdapter;
import hcmute.truongtrangiahung.cuoiky.Model.DanhMuc;
import hcmute.truongtrangiahung.cuoiky.Model.ItemChiTietQuanLy;
import hcmute.truongtrangiahung.cuoiky.Model.SanPham;
import hcmute.truongtrangiahung.cuoiky.R;

public class QuanLyDanhMuc extends AppCompatActivity {
    private ListView listView;
    private ArrayList<ItemChiTietQuanLy> arrayList;
    private ItemChiTietQuanLyAdapter adapter;
    private ImageView imageView, img_Add, img_Edit;

    private ArrayList<DanhMuc> arrayDanhMuc  = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_danh_muc);

        SetID();
        Event();
    }

    private void Event() {
        TaiDuLieu();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //adapter.notifyDataSetChanged();
                Intent intent = new Intent(QuanLyDanhMuc.this, ChiTietDanhMuc.class);
                intent.putExtra("danhMuc", arrayDanhMuc.get(i).getId());
                startActivity(intent);
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        img_Add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(QuanLyDanhMuc.this, ThemDanhMuc.class);
                startActivity(intent);
            }
        });

        img_Edit.setVisibility(View.GONE);

    }

    private void TaiDuLieu() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("DanhMuc");

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                arrayList.clear();
                arrayDanhMuc.clear();
                if(snapshot.getChildrenCount() > 0) {
                    for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
                        DanhMuc danhMuc = new DanhMuc();
                        danhMuc.setTenDanhMuc(dataSnapshot.getValue(String.class));
                        danhMuc.setId(Integer.parseInt(dataSnapshot.getKey()));

                        ItemChiTietQuanLy item = new ItemChiTietQuanLy("0", danhMuc);
                        arrayList.add(item);
                        arrayDanhMuc.add(danhMuc);
                        adapter.notifyDataSetChanged();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void SetID() {
        listView = findViewById(R.id.list_QuanLySanPham);
        imageView = findViewById(R.id.img_Back);
        img_Add = findViewById(R.id.img_Add);
        img_Edit = findViewById(R.id.img_Edit);
        arrayList = new ArrayList<>();
        adapter = new ItemChiTietQuanLyAdapter(this, arrayList, -1);
        listView.setAdapter(adapter);
    }
}