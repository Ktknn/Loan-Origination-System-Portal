import Calendar from 'react-calendar';
import 'react-calendar/dist/Calendar.css';
import './DatePickerPopup.css';

interface Props {
  value: Date | null;
  onChange: (date: Date) => void;
  onClose: () => void;
}

export const DatePickerPopup = ({ value, onChange, onClose }: Props) => {
  return (
    <div
      className="dob-picker-wrapper"
      onMouseDown={e => e.stopPropagation()}
    >
      <Calendar
        value={value}
        onChange={(val) => {
          onChange(val as Date);
          onClose();
        }}
        maxDate={new Date()}
        defaultView='month'

        locale="en-US"
        formatShortWeekday={(_, date) =>
          ['SU', 'MO', 'TU', 'WE', 'TH', 'FR', 'SA'][date.getDay()]
        }
      />
    </div>
  );
};
