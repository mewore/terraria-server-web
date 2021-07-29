package io.github.mewore.tsw.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Entity
@Table(name = "account_type")
public class AccountTypeEntity {

    @Setter(AccessLevel.NONE)
    @Id
    @GeneratedValue
    private Long id;

    @Builder.Default
    @Column(nullable = false)
    private boolean ableToManageAccounts = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean ableToManageHosts = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean ableToManageTerraria = false;
}
