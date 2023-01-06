package id.kawahedukasi.batch5.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "item")
public class Item extends HistoryModel {
    @Id
    @SequenceGenerator(
            name = "itemSequence",
            sequenceName = "item_sequence",
            initialValue = 1,
            allocationSize = 1
    )
    @GeneratedValue(generator = "itemSequence", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    public Long id;

    @Column(name = "name", nullable = false, length = 50, unique = true)
    public String name;

    @Column(name = "count")
    public Integer count;

    @Column(name = "price", precision = 10, scale = 2)
    public BigDecimal price;

    @Column(name = "type", nullable = false, length = 50)
    public String type;

    @Column(name = "description")
    public String description;

}
