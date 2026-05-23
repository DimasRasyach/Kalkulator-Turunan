package com.example.aplikasikalkulatorturunan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull AppCompatActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {

        switch (position) {

            case 0:
                return new TurunanFragment();

            case 1:
                return new LimitFragment();

            case 2:
                return new GrafikFragment();

            default:
                return new TurunanFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}