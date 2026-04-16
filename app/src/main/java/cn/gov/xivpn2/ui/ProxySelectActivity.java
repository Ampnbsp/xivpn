package cn.gov.xivpn2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import cn.gov.xivpn2.R;
import cn.gov.xivpn2.database.AppDatabase;
import cn.gov.xivpn2.database.Proxy;
import cn.gov.xivpn2.database.Subscription;
import cn.gov.xivpn2.xrayconfig.LabelSubscription;

public class ProxySelectActivity extends AppCompatActivity {

    public static final String EXTRA_MULTI = "multi";
    public static final String EXTRA_EXCLUDE_PROTOCOLS = "EXCLUDE_PROTOCOLS";

    public static final String RESULT_LABEL = "SELECTED_LABEL";
    public static final String RESULT_SUBSCRIPTION = "SELECTED_SUBSCRIPTION";
    public static final String RESULT_LABELS = "SELECTED_LABELS";
    public static final String RESULT_SUBSCRIPTIONS = "SELECTED_SUBSCRIPTIONS";

    private boolean isMulti;
    private Set<String> excludeProtocols;
    private final List<Proxy> currentTabProxies = new ArrayList<>();
    private final Set<LabelSubscription> selected = new HashSet<>();

    private ProxySelectAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BlackBackground.apply(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_proxy_select);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.select_proxy);
        }

        // intent
        isMulti = getIntent().getBooleanExtra(EXTRA_MULTI, false);
        ArrayList<String> excludeList = getIntent().getStringArrayListExtra(EXTRA_EXCLUDE_PROTOCOLS);
        excludeProtocols = excludeList != null ? new HashSet<>(excludeList) : new HashSet<>();

        // views
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        MaterialButton btnSelectAll = findViewById(R.id.btn_select_all);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);

        if (isMulti) {
            btnSelectAll.setVisibility(View.VISIBLE);
            btnSelectAll.setOnClickListener(v -> {
                for (int i = 0; i < currentTabProxies.size(); i++) {
                    Proxy p = currentTabProxies.get(i);
                    if (selected.add(new LabelSubscription(p.label, p.subscription))) {
                        adapter.notifyItemChanged(i);
                    }
                }
            });
        }

        // recyclerview
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProxySelectAdapter();
        recyclerView.setAdapter(adapter);

        // tabs
        TabLayout.Tab noneTab = tabLayout.newTab();
        noneTab.setText(R.string.none_subscription);
        noneTab.setTag("none");
        tabLayout.addTab(noneTab);

        List<Subscription> subscriptions = AppDatabase.getInstance().subscriptionDao().findAll();
        for (Subscription sub : subscriptions) {
            TabLayout.Tab tab = tabLayout.newTab();
            tab.setText(sub.label);
            tab.setTag(sub.label);
            tabLayout.addTab(tab);
        }

        loadProxiesForTab(Objects.requireNonNull(tabLayout.getTabAt(0)));

        // tab listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadProxiesForTab(tab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isMulti) {
            getMenuInflater().inflate(R.menu.proxy_select_activity, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.done) {
            ArrayList<String> labels = new ArrayList<>();
            ArrayList<String> subscriptions = new ArrayList<>();
            for (LabelSubscription ls : selected) {
                labels.add(ls.label);
                subscriptions.add(ls.subscription);
            }
            Intent data = new Intent();
            data.putStringArrayListExtra(RESULT_LABELS, labels);
            data.putStringArrayListExtra(RESULT_SUBSCRIPTIONS, subscriptions);
            setResult(RESULT_OK, data);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadProxiesForTab(TabLayout.Tab tab) {
        String subscriptionKey = (String) tab.getTag();
        if (subscriptionKey == null) subscriptionKey = "none";

        List<Proxy> raw = AppDatabase.getInstance().proxyDao().findBySubscription(subscriptionKey);

        int oldSize = currentTabProxies.size();
        currentTabProxies.clear();
        adapter.notifyItemRangeRemoved(0, oldSize);

        for (Proxy p : raw) {
            if (!excludeProtocols.contains(p.protocol)) {
                currentTabProxies.add(p);
            }
        }

        adapter.notifyItemRangeInserted(0, currentTabProxies.size());
    }

    private class ProxySelectAdapter extends RecyclerView.Adapter<ProxySelectAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_proxy_select, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Proxy proxy = currentTabProxies.get(position);
            LabelSubscription key = new LabelSubscription(proxy.label, proxy.subscription);

            holder.card.setCheckable(true);
            holder.card.setChecked(selected.contains(key));

            holder.tvLabel.setText(proxy.label);
            holder.tvProtocol.setText(proxy.protocol);

            if (proxy.subscription.equals("none")) {
                holder.tvSubscription.setVisibility(View.GONE);
            } else {
                holder.tvSubscription.setVisibility(View.VISIBLE);
                holder.tvSubscription.setText(proxy.subscription);
            }

            if (isMulti) {
                holder.card.setOnClickListener((v) -> {
                    if (selected.contains(key)) {
                        selected.remove(key);
                    } else {
                        selected.add(key);
                    }

                    notifyItemChanged(position);
                });

            } else {
                holder.card.setOnClickListener(v -> {
                    Intent data = new Intent();
                    data.putExtra(RESULT_LABEL, proxy.label);
                    data.putExtra(RESULT_SUBSCRIPTION, proxy.subscription);
                    setResult(RESULT_OK, data);
                    finish();
                });
            }
        }

        @Override
        public int getItemCount() {
            return currentTabProxies.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            final TextView tvLabel;
            final TextView tvSubscription;
            final TextView tvProtocol;
            final View itemView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                this.itemView = itemView;
                card = itemView.findViewById(R.id.card);
                tvLabel = itemView.findViewById(R.id.label);
                tvProtocol = itemView.findViewById(R.id.protocol);
                tvSubscription = itemView.findViewById(R.id.subscription);
            }
        }
    }
}
