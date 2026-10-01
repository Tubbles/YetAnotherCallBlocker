package dummydomain.yetanothercallblocker;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;

import dummydomain.yetanothercallblocker.data.db.BlacklistItem;

public class BlacklistItemRecyclerViewAdapter extends GenericRecyclerViewAdapter
        <BlacklistItem, BlacklistItemRecyclerViewAdapter.ViewHolder> {

    public interface ReorderListener {
        void moveUp(BlacklistItem item);

        void moveDown(BlacklistItem item);
    }

    private SelectionTracker<Long> selectionTracker;
    private ReorderListener reorderListener;

    public BlacklistItemRecyclerViewAdapter(
            @Nullable ListInteractionListener<BlacklistItem> listener) {
        super(new DiffUtilCallback(), listener);
    }

    public void setSelectionTracker(SelectionTracker<Long> selectionTracker) {
        this.selectionTracker = selectionTracker;
    }

    public void setReorderListener(ReorderListener reorderListener) {
        this.reorderListener = reorderListener;
    }

    public ItemKeyProvider<Long> getItemKeyProvider() {
        return new ItemKeyProvider<Long>(ItemKeyProvider.SCOPE_MAPPED) {
            @Nullable
            @Override
            public Long getKey(int position) {
                BlacklistItem item = getItem(position);
                return item != null ? item.getId() : null;
            }

            @Override
            public int getPosition(@NonNull Long key) {
                for (int i = 0; i < getItemCount(); i++) {
                    BlacklistItem item = getItem(i);
                    if (item != null && key.equals(item.getId())) return i;
                }
                return RecyclerView.NO_POSITION;
            }
        };
    }

    public ItemDetailsLookup<Long> getItemDetailsLookup(RecyclerView recyclerView) {
        return new ItemDetailsLookup<Long>() {
            @Nullable
            @Override
            public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
                View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (view != null) {
                    RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
                    if (holder instanceof BlacklistItemRecyclerViewAdapter.ViewHolder) {
                        return ((BlacklistItemRecyclerViewAdapter.ViewHolder) holder).getItemDetails();
                    }
                }
                return null;
            }
        };
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.blacklist_item, parent, false);
        return new BlacklistItemRecyclerViewAdapter.ViewHolder(view);
    }

    class ViewHolder extends GenericRecyclerViewAdapter
            <BlacklistItem, BlacklistItemRecyclerViewAdapter.ViewHolder>.GenericViewHolder {

        final TextView name, pattern, stats;
        final AppCompatImageView errorIcon;
        final AppCompatImageButton moveUp, moveDown;

        ItemDetailsLookup.ItemDetails<Long> itemDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            pattern = itemView.findViewById(R.id.pattern);
            stats = itemView.findViewById(R.id.stats);
            errorIcon = itemView.findViewById(R.id.errorIcon);
            moveUp = itemView.findViewById(R.id.moveUp);
            moveDown = itemView.findViewById(R.id.moveDown);

            moveUp.setOnClickListener(v -> onReorderClicked(true));
            moveDown.setOnClickListener(v -> onReorderClicked(false));
        }

        private void onReorderClicked(boolean up) {
            if (reorderListener == null) return;

            int position = getBindingAdapterPosition();
            BlacklistItem item = position != RecyclerView.NO_POSITION ? getItem(position) : null;
            if (item == null) return;

            if (up) {
                reorderListener.moveUp(item);
            } else {
                reorderListener.moveDown(item);
            }
        }

        @Override
        void bind(BlacklistItem item) {
            if (item == null) { // placeholder
                name.setVisibility(View.INVISIBLE);
                pattern.setVisibility(View.INVISIBLE);
                stats.setVisibility(View.GONE);
                errorIcon.setVisibility(View.GONE);
                moveUp.setVisibility(View.INVISIBLE);
                moveDown.setVisibility(View.INVISIBLE);
                itemView.setActivated(false);

                return;
            }

            name.setText(item.getName());
            name.setVisibility(TextUtils.isEmpty(item.getName()) ? View.GONE : View.VISIBLE);

            // the action is what the reader needs first, so it colours the pattern itself
            pattern.setText(item.getPattern());
            pattern.setTextColor(UiUtils.getColorInt(pattern.getContext(),
                    item.getAllow() ? R.color.ratePositive : R.color.rateNegative));
            pattern.setVisibility(View.VISIBLE);

            moveUp.setVisibility(View.VISIBLE);
            moveDown.setVisibility(View.VISIBLE);

            if (item.getNumberOfCalls() > 0) {
                stats.setVisibility(View.VISIBLE);

                Context context = stats.getContext();

                Date lastCallDate = item.getLastCallDate();
                String dateString = lastCallDate != null
                        ? DateUtils.getRelativeTimeSpanString(lastCallDate.getTime()).toString()
                        : context.getString(R.string.blacklist_item_date_no_info);

                stats.setText(context.getResources().getQuantityString(
                        R.plurals.blacklist_item_stats, item.getNumberOfCalls(),
                        item.getNumberOfCalls(), dateString));
            } else {
                stats.setVisibility(View.GONE);
            }

            errorIcon.setVisibility(item.getInvalid() ? View.VISIBLE : View.GONE);

            itemView.setActivated(selectionTracker != null
                    && selectionTracker.isSelected(item.getId()));
        }

        ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
            if (itemDetails == null) {
                itemDetails = new ItemDetailsLookup.ItemDetails<Long>() {
                    @Override
                    public int getPosition() {
                        return getAdapterPosition();
                    }

                    @Nullable
                    @Override
                    public Long getSelectionKey() {
                        int position = getAdapterPosition();
                        BlacklistItem item = position != RecyclerView.NO_POSITION
                                ? getItem(position) : null;
                        return item != null ? item.getId() : null;
                    }
                };
            }
            return itemDetails;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public String toString() {
            return super.toString() + " '" + pattern.getText() + "'";
        }

    }

    static class DiffUtilCallback extends DiffUtil.ItemCallback<BlacklistItem> {

        @Override
        public boolean areItemsTheSame(@NonNull BlacklistItem oldItem,
                                       @NonNull BlacklistItem newItem) {
            if (oldItem.getId() != null || newItem.getId() != null) {
                return ObjectsCompat.equals(oldItem.getId(), newItem.getId());
            }

            return ObjectsCompat.equals(oldItem.getPattern(), newItem.getPattern());
        }

        @Override
        public boolean areContentsTheSame(@NonNull BlacklistItem oldItem,
                                          @NonNull BlacklistItem newItem) {
            return ObjectsCompat.equals(oldItem.getPattern(), newItem.getPattern())
                    && ObjectsCompat.equals(oldItem.getName(), newItem.getName())
                    && oldItem.getAllow() == newItem.getAllow()
                    && oldItem.getPosition() == newItem.getPosition()
                    && oldItem.getNumberOfCalls() == newItem.getNumberOfCalls()
                    && ObjectsCompat.equals(oldItem.getLastCallDate(), newItem.getLastCallDate());
        }

    }

}
